<script setup lang="ts">
import { nextTick, ref, watch } from "vue";
import { useRoute } from "vue-router";
import AppIcon from "@/shared/components/AppIcon.vue";
import AsyncState from "@/shared/components/AsyncState.vue";
import { safeNavigationUrl } from "@/shared/utils/url";
import { isPreview } from "@/shared/config";
import { getCourse, listGames, launchGame } from "@/features/courses/api";
import {
  courseImage,
  subjectName,
  type Course,
  type Game,
} from "@/features/courses/types";
import GameLesson from "./components/GameLesson.vue";

const route = useRoute();
const course = ref<Course>();
const games = ref<Game[]>([]);
const loading = ref(true);
const error = ref("");
const launchingId = ref("");
const launchError = ref("");
const previewGame = ref<Game>();
const previewDialog = ref<HTMLDialogElement>();
let request = 0;

watch(previewGame, async (game) => {
  await nextTick();
  if (game) previewDialog.value?.showModal();
  else if (previewDialog.value?.open) previewDialog.value.close();
});

async function load() {
  const current = ++request;
  loading.value = true;
  error.value = "";
  course.value = undefined;
  games.value = [];
  try {
    const detail = await getCourse(String(route.params.id));
    const availableGames = detail.gameIds?.length ? await listGames() : [];
    if (current !== request) return;
    course.value = detail;
    games.value = (detail.gameIds ?? []).flatMap((id) =>
      availableGames.filter((game) => game.id === id),
    );
  } catch (cause) {
    if (current === request)
      error.value =
        cause instanceof Error ? cause.message : "课程加载失败，请重试。";
  } finally {
    if (current === request) loading.value = false;
  }
}

async function enterGame(id: string) {
  if (launchingId.value) return;
  launchError.value = "";
  if (isPreview) {
    previewGame.value = games.value.find((game) => game.id === id);
    return;
  }
  launchingId.value = id;
  try {
    const result = await launchGame(id);
    const destination = safeNavigationUrl(result.url);
    if (!destination) throw new Error("游戏入口地址无效，请联系老师。");
    window.location.assign(destination);
  } catch (cause) {
    launchError.value =
      cause instanceof Error ? cause.message : "游戏启动失败，请稍后重试。";
  } finally {
    launchingId.value = "";
  }
}

watch(() => route.params.id, load, { immediate: true });
</script>

<template>
  <div class="page-stack">
    <RouterLink to="/courses" class="back-link"
      ><AppIcon name="arrow-left" :size="16" /> 返回课程目录</RouterLink
    ><AsyncState :loading="loading" :error="error" @retry="load"
      ><template v-if="course"
        ><section class="detail-hero">
          <div class="detail-hero-copy">
            <p class="eyebrow">
              {{ subjectName(course.subject) }} <span> / </span> 第
              {{ course.volume }} 册
            </p>
            <h1>{{ course.name }}</h1>
            <p>
              {{
                course.description || "和好奇心一起出发，探索新的知识与能力。"
              }}
            </p>
            <div class="course-tags">
              <span v-if="course.stage"
                ><AppIcon name="layers" :size="13" />{{ course.stage }}</span
              ><span v-if="course.ability"
                ><AppIcon name="sparkles" :size="13" />{{
                  course.ability
                }}</span
              ><span
                ><AppIcon name="grid" :size="13" />{{
                  games.length
                }}
                个探索关卡</span
              >
            </div>
          </div>
          <img :src="courseImage(course.volume)" alt="" />
        </section>
        <div class="detail-grid">
          <section class="lessons-section">
            <div class="section-heading">
              <div>
                <h2>你的探索路线</h2>
                <p class="muted">按自己的节奏，发现每一关的乐趣。</p>
              </div>
              <span class="lesson-count">{{ games.length }} 个关卡</span>
            </div>
            <p v-if="launchError" role="alert" class="launch-error">
              {{ launchError }}
            </p>
            <div v-if="games.length" class="lessons-list">
              <GameLesson
                v-for="(game, index) in games"
                :key="game.id"
                :game="game"
                :index="index"
                :busy="launchingId === game.id"
                @launch="enterGame"
              />
            </div>
            <div v-else class="empty-lessons card">
              <AppIcon name="book" :size="30" />
              <h3>探索内容正在准备中</h3>
              <p>当前课程暂无可进入的已授权游戏，请稍后再来看看。</p>
            </div>
          </section>
          <aside class="detail-aside">
            <section class="card tip-card">
              <span class="tip-icon"
                ><AppIcon name="sparkles" :size="21"
              /></span>
              <h3>给小小探索者的提示</h3>
              <p>
                遇到难题时，先观察，再尝试。每一次思考，都会让你离答案更近一点。
              </p>
              <RouterLink
                :to="{
                  path: '/companion',
                  query: games[0] ? { game: games[0].code } : {},
                }"
                class="tip-link"
                >问问智能学伴 <AppIcon name="arrow-right" :size="15"
              /></RouterLink>
            </section>
            <section class="detail-note">
              <AppIcon name="info" :size="16" />
              <p>
                {{
                  isPreview
                    ? "此页面为设计预览，游戏入口不会提交真实学习记录。"
                    : "游戏入口由平台校验权限后开启，学习记录以服务端实际结果为准。"
                }}
              </p>
            </section>
          </aside>
        </div></template
      ></AsyncState
    >
    <dialog
      ref="previewDialog"
      class="preview-dialog"
      aria-labelledby="preview-game-title"
      @close="previewGame = undefined"
    >
      <template v-if="previewGame"
        ><span class="preview-dialog-icon"
          ><AppIcon name="play" :size="28" /></span
        ><span class="badge">设计预览</span>
        <h2 id="preview-game-title">{{ previewGame.name }}</h2>
        <p>
          课程与游戏入口已准备就绪。连接真实教育平台后，这里会通过授权票据进入游戏。
        </p>
        <p class="preview-detail">当前预览不会生成游戏成绩或学习记录。</p>
        <button
          class="btn btn-primary"
          autofocus
          @click="previewGame = undefined"
        >
          知道了，继续探索
        </button></template
      >
    </dialog>
  </div>
</template>

<style scoped lang="less">
.back-link {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--muted);
  font-size: 11px;
  text-decoration: none;
  align-self: flex-start;
}
.detail-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  overflow: hidden;
  background: linear-gradient(110deg, #e8efff, #eeeaff 75%, #e2ebff);
  border: 1px solid #e1e7ff;
  border-radius: 24px;
  padding: 32px 35px;
  min-height: 213px;
}
.detail-hero-copy {
  position: relative;
  z-index: 1;
  width: 74%;
  > .eyebrow {
    color: #6d7cc5;
    font-size: 10px;
    span {
      margin: 0 7px;
      opacity: 0.45;
    }
  }
  h1 {
    font-size: 28px;
    margin: 13px 0 11px;
    line-height: 1.5;
    color: #354b9a;
  }
  > p:not(.eyebrow) {
    color: #8190ba;
    font-size: 12px;
    line-height: 1.8;
    max-width: 450px;
    margin: 0;
  }
}
.detail-hero > img {
  position: absolute;
  right: 5%;
  height: 193px;
  width: 195px;
  object-fit: contain;
  filter: drop-shadow(0 15px 15px #7180ad22);
  transform: rotate(6deg);
}
.course-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 9px;
  margin-top: 23px;
  span {
    font-size: 10px;
    color: #7c88b8;
    border: 1px solid #ffffffb3;
    border-radius: 7px;
    background: #ffffff83;
    padding: 7px 9px;
    display: flex;
    gap: 5px;
    align-items: center;
  }
}
.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(235px, 1fr);
  gap: 25px;
  margin-top: 28px;
}
.lessons-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.lesson-count {
  font-size: 10px;
  color: #97a2ba;
}
.detail-aside {
  padding-top: 3px;
}
.tip-card {
  padding: 25px !important;
  background: #f7f5ff !important;
  border-color: #ede7fb !important;
  h3 {
    font-size: 14px;
    margin: 14px 0 10px;
  }
  p {
    font-size: 11px;
    color: #9195b5;
    line-height: 2;
    margin: 0;
  }
}
.tip-icon {
  display: grid;
  place-items: center;
  width: 41px;
  height: 41px;
  background: #eae4ff;
  border-radius: 13px;
  color: #9c83db;
}
.tip-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  text-decoration: none;
  color: #8573c8;
  font-size: 11px;
  font-weight: 700;
  margin-top: 22px;
}
.detail-note {
  display: flex;
  gap: 9px;
  color: #a1abc0;
  margin: 18px 8px;
  > svg {
    flex-shrink: 0;
    margin-top: 3px;
  }
  p {
    font-size: 10px;
    line-height: 1.9;
    margin: 0;
  }
}
.empty-lessons {
  text-align: center;
  padding: 42px 20px !important;
  color: #b3bfd5;
  h3 {
    font-size: 14px;
    color: var(--ink);
    margin: 17px 0 9px;
  }
  p {
    font-size: 11px;
    color: var(--muted);
    line-height: 1.8;
    margin: 0;
  }
}
.launch-error {
  background: #fff1f2;
  border: 1px solid #ffdadd;
  border-radius: 11px;
  padding: 13px 16px;
  font-size: 12px;
  line-height: 1.7;
  color: #c35d6a;
}
.preview-dialog {
  border: 0;
  width: min(calc(100% - 44px), 390px);
  &::backdrop {
    background: #1b275b50;
    backdrop-filter: blur(6px);
  }
  box-sizing: border-box;
  border-radius: 25px;
  background: white;
  padding: 32px;
  text-align: center;
  box-shadow: 0 20px 80px #19275b33;
  .badge {
    font-size: 10px;
    margin-top: 17px;
    display: inline-block;
  }
  h2 {
    font-size: 23px;
    margin: 15px 0 12px;
  }
  p {
    color: var(--muted);
    font-size: 12px;
    line-height: 1.9;
    margin: 0;
  }
  .preview-detail {
    font-size: 10px;
    margin: 10px 0 25px;
    color: #a9b2c9;
  }
  .btn {
    width: 100%;
    justify-content: center;
  }
}
.preview-dialog-icon {
  display: grid;
  place-items: center;
  width: 68px;
  height: 68px;
  border-radius: 21px;
  background: #edf1ff;
  color: var(--primary);
  margin: 0 auto;
}
@media (max-width: 850px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
  .detail-aside {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 15px;
    align-items: center;
  }
  .detail-note {
    margin: 0;
  }
  .detail-hero > img {
    right: 0;
    opacity: 0.75;
  }
}
@media (max-width: 600px) {
  .detail-hero {
    padding: 24px;
    min-height: 244px;
  }
  .detail-hero-copy {
    width: 90%;
    h1 {
      font-size: 23px;
    }
    > p:not(.eyebrow) {
      font-size: 11px;
      max-width: 220px;
    }
  }
  .detail-hero > img {
    right: -18%;
    height: 155px;
    opacity: 0.45;
    bottom: 10px;
  }
  .course-tags {
    margin-top: 20px;
    gap: 6px;
    span {
      font-size: 9px;
      padding: 6px;
    }
  }
  .detail-aside {
    grid-template-columns: 1fr;
  }
  .detail-grid {
    gap: 18px;
    margin-top: 22px;
  }
  .detail-note {
    margin: 0 6px;
  }
  .preview-dialog {
    padding: 27px;
  }
}
</style>
