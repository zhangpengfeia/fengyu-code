from typing import Annotated

from fastapi import Depends, Request

from src.app.container import Container
from src.schemas.api import Principal
from src.security.auth import current_principal


def get_container(request: Request) -> Container:
    return request.app.state.container


Services = Annotated[Container, Depends(get_container)]
Student = Annotated[Principal, Depends(current_principal)]
