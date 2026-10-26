import asyncio
import logging

from student_game_agent.app.container import Container
from student_game_agent.config.settings import Settings


async def main() -> None:
    container = Container(Settings())
    try:
        while True:
            processed = await container.evaluations.run_once()
            if not processed:
                await asyncio.sleep(container.settings.worker_poll_seconds)
    finally:
        await container.close()


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    asyncio.run(main())
