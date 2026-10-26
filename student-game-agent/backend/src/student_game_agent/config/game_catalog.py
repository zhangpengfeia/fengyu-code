from pathlib import Path

import yaml
from pydantic import BaseModel, ConfigDict, Field

from student_game_agent.utils.errors import AppError


class Game(BaseModel):
    model_config = ConfigDict(extra="forbid")
    code: str = Field(pattern=r"^[a-z][a-z0-9-]{0,63}$")
    name: str = Field(min_length=1, max_length=100)
    prompt_key: str = Field(min_length=1, max_length=200)
    prompt_version: str = Field(min_length=1, max_length=100)
    enabled: bool = True


class Catalog:
    def __init__(self, games: list[Game]):
        self.games = {game.code: game for game in games}
        if len(self.games) != len(games):
            raise ValueError("游戏 code 不允许重复")

    @classmethod
    def load(cls, path: Path) -> "Catalog":
        content = yaml.safe_load(path.read_text(encoding="utf-8"))
        return cls([Game.model_validate(value) for value in content["games"]])

    def get(self, code: str) -> Game:
        game = self.games.get(code)
        if not game or not game.enabled:
            raise AppError("GAME_NOT_FOUND", "游戏不存在或尚未开放", 404)
        return game
