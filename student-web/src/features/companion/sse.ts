export interface ServerEvent {
  event: string;
  data: string;
}

/** Incremental SSE parser. A transport chunk does not necessarily end on a line. */
export function createSseParser(onEvent: (event: ServerEvent) => void) {
  let buffer = "";
  let event = "message";
  let lines: string[] = [];

  function dispatch() {
    if (lines.length) onEvent({ event, data: lines.join("\n") });
    event = "message";
    lines = [];
  }

  function line(value: string) {
    if (value === "") return dispatch();
    if (value.startsWith(":")) return;
    const separator = value.indexOf(":");
    const field = separator < 0 ? value : value.slice(0, separator);
    let content = separator < 0 ? "" : value.slice(separator + 1);
    if (content.startsWith(" ")) content = content.slice(1);
    if (field === "event") event = content || "message";
    if (field === "data") lines.push(content);
  }

  function consume(final = false) {
    while (buffer.length) {
      const index = buffer.search(/[\r\n]/);
      if (index < 0) break;
      // A CRLF can be divided between two network chunks.
      if (buffer[index] === "\r" && index === buffer.length - 1 && !final)
        break;
      const width =
        buffer[index] === "\r" && buffer[index + 1] === "\n" ? 2 : 1;
      line(buffer.slice(0, index));
      buffer = buffer.slice(index + width);
    }
    if (final && buffer) {
      line(buffer);
      buffer = "";
    }
    if (final) dispatch();
  }

  return {
    push(chunk: string) {
      buffer += chunk;
      consume();
    },
    finish() {
      consume(true);
    },
  };
}

export async function readEventStream(
  response: Response,
  onEvent: (event: ServerEvent) => void,
) {
  if (!response.body)
    throw new Error("当前浏览器无法读取消息流，请更换浏览器后重试。");
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  const parser = createSseParser(onEvent);
  let completed = false;
  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) {
        completed = true;
        break;
      }
      parser.push(decoder.decode(value, { stream: true }));
    }
    parser.push(decoder.decode());
    parser.finish();
  } finally {
    if (!completed) await reader.cancel().catch(() => undefined);
    reader.releaseLock();
  }
}
