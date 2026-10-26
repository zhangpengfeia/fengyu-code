export function safeNavigationUrl(
  value: string,
  origin = window.location.origin,
): string {
  const url = new URL(value, origin);
  if (
    !["http:", "https:"].includes(url.protocol) ||
    url.username ||
    url.password
  ) {
    throw new Error("游戏入口地址无效，请联系老师。");
  }
  return url.href;
}
