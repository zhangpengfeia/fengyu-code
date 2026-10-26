import secrets
from datetime import UTC, datetime, timedelta
from typing import Annotated
from uuid import UUID, uuid4

import jwt
from fastapi import APIRouter, Depends, Header, Request
from pydantic import Field

from src.app.dependencies import Services, Student
from src.schemas.api import AttemptData, StrictModel, TokenRequest
from src.security.auth import issue_token
from src.utils.errors import AppError

router = APIRouter(tags=["mock-development"])


def envelope(data):
    return {"code": 200, "msg": "Mock 联调数据", "data": data}


def education_claims(request: Request, authorization: str | None = Header(default=None)):
    container = request.app.state.container
    try:
        if not authorization or not authorization.startswith("Bearer "):
            raise ValueError("missing token")
        claims = jwt.decode(
            authorization[7:],
            container.settings.jwt_secret.get_secret_value(),
            algorithms=["HS256"],
            audience="mock-education",
            issuer="mock-education",
            options={"require": ["sub", "exp", "jti"]},
        )
        if claims["sub"] != container.mock.data["student"]["id"]:
            raise ValueError("unknown student")
        if claims["jti"] in container.mock.revoked_tokens:
            raise ValueError("logged out")
        return claims
    except (ValueError, jwt.PyJWTError, TypeError, KeyError) as exc:
        raise AppError("UNAUTHORIZED", "Mock 登录已过期，请重新登录", 401) from exc


class MockLogin(StrictModel):
    mode: str
    username: str
    password: str
    school_id: str = ""
    code: str = ""
    uuid: str = ""


class MockFinish(StrictModel):
    request_id: UUID
    game_code: str = Field(pattern=r"^[a-z][a-z0-9-]{0,63}$")


class MockWidgetInput(StrictModel):
    request_id: UUID
    facts: AttemptData


@router.post("/dev/mock/widget-session")
async def widget_session(body: MockWidgetInput, services: Services):
    """Explicit development-only bootstrap; this route is absent in live mode."""
    user = services.mock.principal.model_copy(
        update={"app_id": services.settings.integration_app_id}
    )
    facts = body.facts
    if (facts.student_id, facts.school_id) != (user.student_id, user.school_id):
        raise AppError("FORBIDDEN", "只能传入当前模拟学生的数据", 403)
    attempt_id = "planet-widget-" + str(body.request_id)
    game = services.catalog.get(facts.game_code)
    if (
        facts.source_attempt_id != attempt_id
        or facts.source_revision != 1
        or facts.game_rule_version != game.prompt_version
    ):
        raise AppError("INVALID_ATTEMPT_DATA", "模拟局次或规则版本不匹配", 422)
    supplied = facts.model_dump(mode="json")
    job_id = None
    accepted_facts = supplied
    if facts.mode == "ASSESSMENT":
        job_id = await services.mock.create_attempt(
            services, user, facts.game_code, attempt_id, case=supplied
        )
        job = await services.evaluation_repository.student_job(user, job_id)
        # Reopening a round is idempotent; changed metrics require a new round ID.
        compared = set(AttemptData.model_fields) - {"ended_at"}
        if any(job.facts.get(key) != supplied[key] for key in compared):
            raise AppError(
                "IDEMPOTENCY_CONFLICT", "同一模拟局次不能更换成绩，请发送新一局数据", 409
            )
        accepted_facts = job.facts
    token = issue_token(
        services.settings, TokenRequest(student_id=user.student_id, school_id=user.school_id)
    )
    return {
        "access_token": token,
        "expires_in": services.settings.token_ttl_seconds,
        "evaluation_id": job_id,
        "mock": True,
        "student": {
            "id": user.student_id,
            "name": services.mock.data["student"]["name"],
            "school_id": user.school_id,
        },
        "facts": accepted_facts,
    }


@router.get("/dev/education/captchaImage")
async def captcha():
    return envelope({"captchaEnabled": False, "uuid": "mock-no-captcha"})


@router.post("/dev/education/biz/auth/login")
async def login(body: MockLogin, services: Services):
    data = services.mock.data
    student = data["student"]
    if (
        body.mode not in {"ACCOUNT", "STUDENT"}
        or body.username != student["username"]
        or not secrets.compare_digest(body.password, student["password"])
        or (body.mode == "STUDENT" and body.school_id != data["school"]["id"])
    ):
        raise AppError("INVALID_CREDENTIALS", "Mock 账号、密码或学校编号不正确", 401)
    now = datetime.now(UTC)
    token = jwt.encode(
        {
            "sub": student["id"],
            "iss": "mock-education",
            "aud": "mock-education",
            "iat": now,
            "exp": now + timedelta(hours=4),
            "jti": str(uuid4()),
        },
        services.settings.jwt_secret.get_secret_value(),
        algorithm="HS256",
    )
    return envelope({"token": token, "mock": True})


@router.get("/dev/education/biz/auth/me", dependencies=[Depends(education_claims)])
async def me(services: Services):
    data = services.mock.data
    return envelope(
        {
            "user_id": data["student"]["id"],
            "name": data["student"]["name"],
            "school_id": data["school"]["id"],
            "must_change_pwd": False,
            "phone_verified": False,
        }
    )


@router.get("/dev/education/biz/options", dependencies=[Depends(education_claims)])
async def options(services: Services):
    data = services.mock.data
    return envelope(
        {
            "schools": [{"school_id": data["school"]["id"], "school_name": data["school"]["name"]}],
            "classes": [
                {
                    "class_id": data["class"]["id"],
                    "class_name": data["class"]["name"],
                    "school_id": data["school"]["id"],
                }
            ],
        }
    )


@router.post("/dev/education/biz/auth/agent-token", dependencies=[Depends(education_claims)])
async def agent_token(services: Services):
    data = services.mock.data
    token = issue_token(
        services.settings,
        TokenRequest(student_id=data["student"]["id"], school_id=data["school"]["id"]),
    )
    return envelope({"access_token": token, "expires_in": services.settings.token_ttl_seconds})


@router.post("/dev/education/logout")
async def logout(services: Services, claims: Annotated[dict, Depends(education_claims)]):
    services.mock.revoked_tokens.add(claims["jti"])
    return envelope({"logged_out": True})


@router.get("/dev/education/biz/catalog/{kind}", dependencies=[Depends(education_claims)])
async def catalog(kind: str, services: Services, page: int = 1, size: int = 100):
    if page < 1 or size < 1 or size > 100:
        raise AppError("INVALID_PAGE", "分页参数不正确", 422)
    if kind == "courseware":
        rows = services.mock.data["courses"]
    elif kind == "games":
        rows = [
            {
                "game_id": game.code,
                "game_code": game.code,
                "game_name": game.name,
                "description": "Mock 联调关卡，不代表真实玩法",
                "source_type": "MOCK",
            }
            for game in services.catalog.games.values()
            if game.enabled
        ]
    else:
        raise AppError("NOT_FOUND", "Mock 没有此目录", 404)
    return {"code": 200, "rows": rows[(page - 1) * size : page * size], "total": len(rows)}


@router.get(
    "/dev/education/biz/catalog/courseware/{course_id}", dependencies=[Depends(education_claims)]
)
async def course(course_id: str, services: Services):
    row = next((c for c in services.mock.data["courses"] if c["courseware_id"] == course_id), None)
    if not row:
        raise AppError("NOT_FOUND", "Mock 课程不存在", 404)
    return envelope(row)


@router.post("/dev/education/biz/games/{game_id}/launch", dependencies=[Depends(education_claims)])
async def launch(game_id: str, services: Services):
    services.catalog.get(game_id)
    return envelope({"url": "/mock-game/" + game_id, "mock": True})


@router.get("/dev/education/biz/learning/ranking", dependencies=[Depends(education_claims)])
async def ranking(services: Services, class_id: str):
    if class_id != services.mock.data["class"]["id"]:
        raise AppError("FORBIDDEN", "只能查看本人的联调班级", 403)
    return envelope(services.mock.data["ranking"])


@router.post("/dev/mock/game-finish", status_code=202)
async def game_finish(body: MockFinish, user: Student, services: Services):
    job_id = await services.mock.create_attempt(
        services, user, body.game_code, "mock-round-" + str(body.request_id)
    )
    return {"job_id": job_id, "status": "RECEIVED", "mock": True}
