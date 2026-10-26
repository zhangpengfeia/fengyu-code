import { educationRequest } from "@/shared/api/client";
import { isPreview } from "@/shared/config";
import { previewCourses, previewGames } from "./preview";
import type { Course, Game } from "./types";

interface CourseDto {
  courseware_id: string | number;
  courseware_name: string;
  description?: string;
  subject_code: string;
  volume: number;
  stage_code?: string;
  game_ids?: (string | number)[];
  extension?: { stage?: string; ability?: string };
}

interface GameDto {
  game_id: string | number;
  game_code: string;
  game_name: string;
  description?: string;
  source_type?: string;
}

interface Page<T> {
  rows: T[];
  total: number;
}

function mapCourse(row: CourseDto): Course {
  return {
    id: String(row.courseware_id),
    name: row.courseware_name,
    description: row.description ?? "",
    subject: row.subject_code,
    volume: Number(row.volume),
    stage: row.extension?.stage ?? row.stage_code,
    ability: row.extension?.ability,
    gameIds: row.game_ids?.map(String),
  };
}

async function catalog<T>(kind: "games" | "courseware"): Promise<T[]> {
  const rows: T[] = [];
  let page = 1;
  let total = 0;
  do {
    const response = await educationRequest<Page<T>>({
      url: `/biz/catalog/${kind}`,
      params: { page, size: 100, status: "PUBLISHED" },
    });
    rows.push(...response.rows);
    total = response.total;
    if (response.rows.length === 0) break;
    page += 1;
  } while (rows.length < total);
  return rows;
}

export async function listCourses(): Promise<Course[]> {
  if (isPreview) return previewCourses.map((course) => ({ ...course }));
  return (await catalog<CourseDto>("courseware")).map(mapCourse);
}

export async function getCourse(id: string): Promise<Course> {
  if (isPreview) {
    const course = previewCourses.find((item) => item.id === id);
    if (!course) throw new Error("课程不存在，请返回课程目录重新选择。");
    return { ...course };
  }
  return mapCourse(
    await educationRequest<CourseDto>({
      url: `/biz/catalog/courseware/${encodeURIComponent(id)}`,
    }),
  );
}

export async function listGames(): Promise<Game[]> {
  if (isPreview) return previewGames.map((game) => ({ ...game }));
  return (await catalog<GameDto>("games")).map((row) => ({
    id: String(row.game_id),
    code: row.game_code,
    name: row.game_name,
    description: row.description ?? "",
    sourceType: row.source_type,
  }));
}

export async function launchGame(id: string): Promise<{ url: string }> {
  if (isPreview)
    throw new Error("当前为设计预览。连接真实教育平台后，可进入已授权游戏。");
  return educationRequest<{ url: string }>({
    url: `/biz/games/${encodeURIComponent(id)}/launch`,
    method: "POST",
  });
}
