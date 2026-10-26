from typing import Annotated

from fastapi import Depends, Request

from student_game_agent.app.container import Container
from student_game_agent.schemas.api import Principal
from student_game_agent.security.auth import current_principal


def get_container(request: Request) -> Container:
    return request.app.state.container


Services = Annotated[Container, Depends(get_container)]
Student = Annotated[Principal, Depends(current_principal)]
