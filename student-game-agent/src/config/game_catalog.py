import re
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
    aliases: list[str] = Field(default_factory=list, max_length=20)


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

    def mentioned_games(self, question: str) -> list[Game]:
        """使用受控目录识别名称，不增加一次模型分类调用。多个匹配由上层澄清。"""
        matches = []
        text = question.casefold()
        for game in self.games.values():
            if not game.enabled:
                continue
            names = [game.name, *game.aliases]
            if any(name and name.casefold() in text for name in names) or re.search(
                rf"(?<![a-z0-9-]){re.escape(game.code)}(?![a-z0-9-])", text
            ):
                matches.append(game)
        return matches
