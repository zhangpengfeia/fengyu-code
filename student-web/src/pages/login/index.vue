<script setup lang="ts">
import StudentLoginForm from "./components/StudentLoginForm.vue";
import AppIcon from "@/shared/components/AppIcon.vue";
import { assetUrl } from "@/shared/utils/assets";
</script>
<template>
  <main class="login-page">
    <section class="login-story">
      <div class="login-brand">
        <span><AppIcon name="sparkles" :size="26" /></span>枫语趣学
      </div>

      <div class="planet-universe" aria-hidden="true">
        <i class="orbit orbit-one" /><i class="orbit orbit-two" /><img
          class="planet-main"
          :src="assetUrl('images/planet-5.webp')"
          alt=""
        /><img
          class="planet-small"
          :src="assetUrl('images/planet-2.webp')"
          alt=""
        /><span class="space-star star-one">✦</span
        ><span class="space-star star-two">✧</span
        ><span class="space-star star-three">✦</span>
      </div>
    </section>
    <section class="login-form-side">
      <StudentLoginForm />
      <footer>枫语趣学 · 学生成长空间</footer>
    </section>
  </main>
</template>
<style scoped lang="less">
.login-page {
  min-height: 100dvh;
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  background: #fff;
}
.login-story {
  position: relative;
  overflow: hidden;
  background: linear-gradient(155deg, #2657ff, #5476ff 50%, #7886ff);
  padding: 40px 8%;
  color: #fff;
  min-height: 740px;
  display: flex;
  flex-direction: column;
}
.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 25px;
  letter-spacing: -0.05em;
  font-weight: 700;
}
.login-brand > span {
  width: 44px;
  height: 44px;
  background: #ffffff24;
  border: 1px solid #ffffff4a;
  border-radius: 13px;
  display: grid;
  place-items: center;
}
.story-copy {
  margin-top: 90px;
  z-index: 1;
}
.story-copy > p {
  font-size: 10px;
  letter-spacing: 0.3em;
  color: #c3d0ff;
  margin-bottom: 22px;
}
.story-copy h1 {
  font-size: clamp(30px, 3vw, 45px);
  line-height: 1.5;
  letter-spacing: -0.05em;
  margin-bottom: 20px;
}
.story-copy > span {
  font-size: 13px;
  color: #d2dcff;
  line-height: 1.8;
}
.planet-universe {
  position: relative;
  height: 310px;
  margin-top: 18px;
}
.planet-main {
  position: absolute;
  width: 215px;
  left: 20%;
  top: 40px;
  filter: drop-shadow(0 18px 30px #162fbf44);
  transform: rotate(-9deg);
}
.planet-small {
  position: absolute;
  width: 100px;
  right: -5%;
  top: 0;
  transform: rotate(18deg);
  opacity: 0.9;
}
.orbit {
  position: absolute;
  border: 1px solid #ffffff22;
  width: 450px;
  height: 170px;
  border-radius: 50%;
  left: -10%;
  top: 100px;
  transform: rotate(-24deg);
}
.orbit-two {
  transform: rotate(24deg);
  width: 520px;
  left: -15%;
  top: 80px;
}
.space-star {
  position: absolute;
  color: #ffdf7c;
  font-size: 35px;
}
.star-one {
  left: 6%;
  top: 55px;
}
.star-two {
  right: 7%;
  top: 190px;
}
.star-three {
  left: 45%;
  top: 285px;
  font-size: 18px;
}
.universe-label {
  display: flex;
  gap: 8px;
  align-items: center;
  position: absolute;
  bottom: 25px;
  left: 8%;
  padding: 13px 19px;
  background: #ffffff1c;
  backdrop-filter: blur(10px);
  border: 1px solid #ffffff38;
  border-radius: 12px;
  font-size: 11px;
  color: #f4f6ff;
}
.story-footer {
  font-size: 10px;
  color: #d0d8ff;
  letter-spacing: 0.1em;
  margin: auto 0 0;
  padding-top: 25px;
}
.login-form-side {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  padding: 60px 35px 90px;
}
.login-form-side > footer {
  position: absolute;
  bottom: 32px;
  color: #a8b1c5;
  font-size: 10px;
}
@media (max-width: 767px) {
  .login-page {
    display: block;
    background: var(--canvas);
  }
  .login-story {
    min-height: auto;
    height: 245px;
    padding: 26px 25px;
    border-radius: 0 0 36px 0;
  }
  .login-brand {
    font-size: 22px;
  }
  .login-brand > span {
    width: 36px;
    height: 36px;
  }
  .story-copy {
    margin-top: 27px;
  }
  .story-copy > p {
    display: none;
  }
  .story-copy h1 {
    font-size: 24px;
    margin-bottom: 8px;
    line-height: 1.5;
  }
  .story-copy > span {
    font-size: 10px;
    max-width: 240px;
    display: block;
  }
  .planet-universe {
    position: absolute;
    right: -12px;
    bottom: 0;
    width: 160px;
    height: 175px;
    margin: 0;
  }
  .planet-main {
    width: 140px;
    top: 10px;
    left: 10px;
  }
  .planet-small,
  .orbit-two,
  .universe-label,
  .star-three {
    display: none;
  }
  .orbit {
    width: 220px;
    height: 90px;
    top: 55px;
    left: -40px;
  }
  .star-one {
    font-size: 22px;
    left: 0;
    top: 0;
  }
  .star-two {
    font-size: 25px;
    right: 13px;
    top: 135px;
  }
  .story-footer {
    display: none;
  }
  .login-form-side {
    padding: 32px 26px 85px;
    min-height: calc(100dvh - 245px);
  }
  .login-form-side > footer {
    bottom: 22px;
  }
}
</style>
