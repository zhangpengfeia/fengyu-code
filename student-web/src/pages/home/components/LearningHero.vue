<script setup lang="ts">
import AppIcon from "@/shared/components/AppIcon.vue";
import { assetUrl } from "@/shared/utils/assets";
</script>

<template>
  <section class="learning-hero">
    <div class="hero-copy">
      <span class="hero-label"><span></span> 每一天，都有新发现</span>
      <h2>让好奇心，<br />带你探索大世界。</h2>
      <p>和智能学伴一起，在游戏中学习，在探索中成长。</p>
      <RouterLink to="/courses" class="hero-button"
        >开启今日探索 <AppIcon name="arrow-right" :size="17"
      /></RouterLink>
    </div>
    <div class="hero-art" aria-hidden="true">
      <span class="orbit orbit-a"></span><span class="orbit orbit-b"></span
      ><span class="hero-spark spark-a">✦</span
      ><span class="hero-spark spark-b">✧</span
      ><span class="hero-spark spark-c">✦</span
      ><img :src="assetUrl('images/planet-5.webp')" alt="" /><span
        class="floating-note"
        ><AppIcon name="sparkles" :size="15" /> 下一个发现，等你解锁</span
      >
    </div>
  </section>
</template>

<style scoped lang="less">
.learning-hero {
  min-height: 273px;
  overflow: hidden;
  position: relative;
  border-radius: 25px;
  background: linear-gradient(112deg, #3565ff 0%, #5c7eff 55%, #9bb3ff 100%);
  color: #fff;
  display: flex;
  align-items: center;
  padding: 30px 35px;
}
.hero-copy {
  position: relative;
  z-index: 1;
  width: 67%;
  h2 {
    font-size: clamp(23px, 2.1vw, 32px);
    line-height: 1.55;
    margin: 14px 0 9px;
    font-weight: 750;
    letter-spacing: 1px;
  }
  p {
    margin: 0 0 23px;
    color: #e0e7ff;
    font-size: 11px;
    line-height: 1.8;
  }
}
.hero-label {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 10px;
  letter-spacing: 1px;
  color: #e1e8ff;
  span {
    height: 5px;
    width: 5px;
    border-radius: 50%;
    background: #cfdaff;
  }
}
.hero-button {
  display: inline-flex;
  align-items: center;
  gap: 19px;
  background: #fff;
  color: #3a62df;
  border-radius: 12px;
  padding: 12px 18px;
  text-decoration: none;
  font-weight: 750;
  font-size: 12px;
  box-shadow: 0 6px 15px #173e9d15;
}
.hero-art {
  position: absolute;
  right: 2%;
  top: 4%;
  width: 43%;
  height: 94%;
  display: grid;
  place-items: center;
  img {
    width: 210px;
    max-width: 90%;
    filter: drop-shadow(0 18px 16px #153e9e34);
    transform: rotate(-9deg);
  }
}
.orbit {
  position: absolute;
  border: 1px solid #ffffff3b;
  width: 260px;
  height: 120px;
  border-radius: 50%;
  transform: rotate(-28deg);
}
.orbit-b {
  width: 220px;
  height: 230px;
  transform: rotate(15deg);
  border-color: #ffffff17;
}
.hero-spark {
  position: absolute;
  color: #ffde8e;
  font-size: 29px;
}
.spark-a {
  left: 16%;
  top: 13%;
}
.spark-b {
  right: 7%;
  top: 28%;
  font-size: 22px;
}
.spark-c {
  right: 20%;
  bottom: 14%;
  font-size: 17px;
}
.floating-note {
  position: absolute;
  bottom: 20px;
  left: 10%;
  display: flex;
  gap: 6px;
  align-items: center;
  padding: 9px 12px;
  border: 1px solid #ffffff70;
  border-radius: 11px;
  font-size: 9px;
  background: #ffffffde;
  color: #6482d6;
  transform: rotate(5deg);
  box-shadow: 0 8px 18px #3256b825;
}
@media (max-width: 600px) {
  .learning-hero {
    padding: 24px;
    min-height: 265px;
  }
  .hero-copy {
    width: 100%;
    h2 {
      font-size: 23px;
    }
    p {
      max-width: 185px;
    }
  }
  .hero-art {
    width: 155px;
    height: 175px;
    right: -20px;
    top: auto;
    bottom: -12px;
    opacity: 0.86;
    img {
      width: 140px;
    }
  }
  .orbit {
    width: 180px;
  }
  .floating-note {
    display: none;
  }
  .hero-button {
    padding: 11px 14px;
  }
}
</style>
