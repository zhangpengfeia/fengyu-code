import { assetUrl } from "@/shared/utils/assets";

export interface Course {
  id: string;
  name: string;
  description: string;
  subject: string;
  volume: number;
  stage?: string;
  ability?: string;
  gameIds?: string[];
  progress?: number;
  tone?: string;
}

export interface Game {
  id: string;
  code: string;
  name: string;
  description: string;
  sourceType?: string;
}

export const subjects = [
  { code: "ALL", name: "全部课程", icon: "grid", tone: "blue" },
  { code: "MATH", name: "趣味数学", icon: "shapes", tone: "orange" },
  { code: "READING", name: "阅读表达", icon: "book", tone: "purple" },
  { code: "SCIENCE", name: "科学探索", icon: "sparkles", tone: "green" },
  { code: "CREATIVE", name: "创意思维", icon: "palette", tone: "pink" },
];

export function subjectName(code: string) {
  return subjects.find((subject) => subject.code === code)?.name ?? "探索课程";
}

export function courseImage(volume: number) {
  return assetUrl(`images/planet-${((Math.max(1, volume) - 1) % 5) + 1}.webp`);
}
