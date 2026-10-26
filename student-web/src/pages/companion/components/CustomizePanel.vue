<script setup lang="ts">
import { reactive, ref } from "vue";
import AppIcon from "@/shared/components/AppIcon.vue";
import MascotAvatar from "@/shared/components/MascotAvatar.vue";
import { useCompanionPreferences } from "@/features/companion/preferences";

const { preferences, save } = useCompanionPreferences();
const form = reactive({ ...preferences.value });
const upload = ref<HTMLInputElement>();
const error = ref("");
const success = ref("");
const uploading = ref(false);

async function changeAvatar(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  error.value = "";
  success.value = "";
  if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
    error.value = "请选择常见格式的照片或图片文件。";
    input.value = "";
    return;
  }
  // Data URLs expand by ~1/3; stay comfortably within browser storage limits.
  if (file.size > 2 * 1024 * 1024) {
    error.value = "图片不能超过两兆字节，请压缩后重试。";
    input.value = "";
    return;
  }
  uploading.value = true;
  try {
    const data = await new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result));
      reader.onerror = () => reject(new Error("图片读取失败，请重新选择。"));
      reader.readAsDataURL(file);
    });
    await new Promise<void>((resolve, reject) => {
      const image = new Image();
      image.onload = () =>
        image.naturalWidth > 0 && image.naturalHeight > 0
          ? resolve()
          : reject(new Error("图片内容无效。"));
      image.onerror = () =>
        reject(new Error("图片内容无效，请选择可正常打开的图片。"));
      image.src = data;
    });
    form.avatar = data;
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "图片读取失败。";
  } finally {
    uploading.value = false;
    input.value = "";
  }
}

function savePreferences() {
  error.value = "";
  success.value = "";
  if (!form.name.trim()) {
    error.value = "给你的学伴起一个名字吧。";
    return;
  }
  try {
    save(form);
    success.value = "已保存到当前浏览器。";
  } catch {
    error.value = "浏览器存储空间不足或已禁用，请缩小头像后重试。";
  }
}
</script>

<template>
  <form class="customize-panel" @submit.prevent="savePreferences">
    <div class="customize-form">
      <section class="customize-card">
        <div class="custom-section-title">
          <span class="step-number">01</span>
          <div>
            <h3>给学伴起个名字</h3>
            <p>一个专属的称呼，让陪伴更亲切。</p>
          </div>
        </div>
        <label class="sr-only" for="companion-name">学伴昵称</label
        ><input
          id="companion-name"
          v-model="form.name"
          class="field"
          maxlength="20"
          placeholder="请输入学伴昵称"
          required
        />
        <p class="field-help">
          最多 20 个字<span>{{ form.name.length }}/20</span>
        </p>
      </section>
      <section class="customize-card">
        <div class="custom-section-title">
          <span class="step-number">02</span>
          <div>
            <h3>选择专属头像</h3>
            <p>用你喜欢的样子，陪你一起学习。</p>
          </div>
        </div>
        <div class="avatar-settings">
          <img
            v-if="form.avatar"
            :src="form.avatar"
            alt="学伴头像预览"
          /><MascotAvatar v-else :size="70" />
          <div>
            <input
              ref="upload"
              class="sr-only"
              type="file"
              accept="image/png,image/jpeg,image/webp"
              aria-label="选择学伴头像"
              @change="changeAvatar"
            /><button
              class="btn btn-soft"
              type="button"
              :disabled="uploading"
              @click="upload?.click()"
            >
              <AppIcon name="upload" :size="15" />{{
                uploading ? "读取中…" : "选择图片"
              }}
            </button>
            <p>支持常见图片格式，大小不超过两兆字节</p>
            <button
              v-if="form.avatar"
              class="remove-avatar"
              type="button"
              @click="form.avatar = ''"
            >
              恢复默认头像
            </button>
          </div>
        </div>
      </section>
      <section class="customize-card">
        <div class="custom-section-title">
          <span class="step-number">03</span>
          <div>
            <h3>偏爱的声音</h3>
            <p>先保存你的选择，等待声音功能开启。</p>
          </div>
        </div>
        <label class="sr-only" for="companion-voice">语音偏好</label
        ><select id="companion-voice" v-model="form.voice" class="field">
          <option value="gentle">温柔陪伴</option>
          <option value="energetic">活力伙伴</option>
          <option value="clear">清晰讲解</option>
        </select>
        <p class="field-help">语音合成服务尚未接入，当前仅保存偏好。</p>
      </section>
      <div class="customize-footer">
        <div>
          <p v-if="error" class="save-error" role="alert">{{ error }}</p>
          <p v-if="success" class="save-success" role="status">
            <AppIcon name="check" :size="15" />{{ success }}
          </p>
        </div>
        <button class="btn btn-primary" type="submit" :disabled="uploading">
          <AppIcon name="check" :size="16" />保存设置
        </button>
      </div>
      <p class="local-note">
        昵称、头像和语音偏好保存在当前浏览器，暂不跨设备同步。头像不会上传到服务器。
      </p>
    </div>
    <aside class="customize-preview">
      <span class="eyebrow">认识你的学习伙伴</span>
      <div class="preview-orbit">
        <img
          v-if="form.avatar"
          :src="form.avatar"
          alt="自定义学伴外观"
        /><MascotAvatar v-else :size="115" /><span class="preview-star">✦</span
        ><span class="preview-dot" />
      </div>
      <h3>你好呀，我是{{ form.name || "枫同学" }}</h3>
      <p>让我们成为学习路上的好伙伴，<br />一起发现更多有趣的答案。</p>
      <span class="preview-tag"
        ><AppIcon name="sparkles" :size="13" />你的专属学伴</span
      >
    </aside>
  </form>
</template>

<style scoped lang="less">
.customize-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 30px;
  padding: 28px;
  background: #fafbff;
  align-items: start;
}
.customize-card {
  background: white;
  border: 1px solid var(--line);
  border-radius: 13px;
  padding: 22px;
  margin-bottom: 16px;
}
.custom-section-title {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 18px;
  h3 {
    margin: 1px 0 7px;
    color: #526489;
    font-size: 13px;
  }
  p {
    margin: 0;
    color: #a1abc2;
    font-size: 11px;
  }
}
.step-number {
  background: #eff2ff;
  color: #91a2e0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 10px;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
}
.field {
  width: 100%;
  background: #fbfcff;
  color: #687b9f;
  border: 1px solid #e0e6f5;
  border-radius: 9px;
  padding: 12px 14px;
  font-size: 12px;
  font-family: inherit;
}
.field-help {
  display: flex;
  justify-content: space-between;
  margin: 9px 0 0;
  font-size: 10px;
  color: #b0b8cb;
}
.avatar-settings {
  display: flex;
  gap: 18px;
  align-items: center;
  > img {
    width: 70px;
    height: 70px;
    border-radius: 50%;
    object-fit: cover;
    border: 3px solid #e9eeff;
  }
  > div p {
    color: #b0b7cb;
    font-size: 10px;
    margin: 10px 0 0;
  }
  .btn {
    font-size: 11px;
    padding: 8px 12px;
  }
}
.remove-avatar {
  border: 0;
  background: none;
  padding: 7px 0 0;
  color: #8899c8;
  font-size: 10px;
  cursor: pointer;
}
.customize-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  > .btn {
    display: flex;
    align-items: center;
    gap: 7px;
    flex-shrink: 0;
  }
}
.save-error {
  font-size: 11px;
  line-height: 1.6;
  color: #bc766e;
}
.save-success {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: #50a391;
}
.local-note {
  margin: 18px 0 0;
  line-height: 1.8;
  color: #aab3c9;
  font-size: 10px;
}
.customize-preview {
  position: sticky;
  top: 20px;
  text-align: center;
  background: linear-gradient(150deg, #edf2ff, #f5efff);
  border-radius: 16px;
  border: 1px solid #e7e9fa;
  padding: 32px 16px;
  .eyebrow {
    font-size: 8px;
    color: #989bc8;
  }
  h3 {
    color: #6473b1;
    font-size: 17px;
    margin: 20px 0 12px;
    overflow-wrap: anywhere;
  }
  p {
    font-size: 11px;
    color: #a2a7c1;
    line-height: 1.9;
  }
}
.preview-orbit {
  width: 180px;
  height: 180px;
  margin: 25px auto 0;
  border: 1px dashed #d4ddf9;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  > img {
    width: 115px;
    height: 115px;
    border-radius: 50%;
    object-fit: cover;
  }
  &::after {
    content: "";
    position: absolute;
    inset: 13px;
    border: 1px solid #e0e4fa;
    border-radius: 50%;
    pointer-events: none;
  }
}
.preview-star {
  position: absolute;
  top: 0;
  left: 11px;
  color: #e8c376;
  font-size: 25px;
}
.preview-dot {
  position: absolute;
  right: 4px;
  bottom: 30px;
  width: 8px;
  height: 8px;
  background: #bdb3ef;
  border-radius: 50%;
}
.preview-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 10px;
  color: #a39bc7;
  background: #ffffff91;
  border-radius: 15px;
  padding: 7px 10px;
  margin-top: 13px;
}
@media (max-width: 900px) {
  .customize-panel {
    grid-template-columns: minmax(0, 1fr);
  }
  .customize-preview {
    display: none;
  }
}
@media (max-width: 640px) {
  .customize-panel {
    padding: 20px 14px;
    gap: 0;
  }
  .customize-card {
    padding: 18px;
  }
  .field {
    font-size: 16px;
  }
  .avatar-settings {
    gap: 12px;
  }
}
</style>
