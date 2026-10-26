<script setup lang="ts">
import { computed } from "vue";
import MedalBadge from "@/features/learning/components/MedalBadge.vue";
import type { RankingEntry } from "@/features/learning/types";

const props = defineProps<{ entries: RankingEntry[] }>();
const ordered = computed(() =>
  [props.entries[1], props.entries[0], props.entries[2]].filter(
    (item): item is RankingEntry => !!item,
  ),
);
const medals = ["GOLD", "SILVER", "BRONZE"] as const;
</script>

<template>
  <div class="podium" aria-label="班级榜前三名">
    <div
      v-for="entry in ordered"
      :key="entry.id"
      class="podium-person"
      :class="`place-${entry.rank}`"
    >
      <div class="avatar-wrap">
        <span v-if="entry.rank === 1" class="crown" aria-hidden="true">♛</span>
        <div class="person-avatar">
          {{ entry.name.slice(0, 1) }}<span class="avatar-star">✦</span>
        </div>
        <span class="person-medal"
          ><MedalBadge :medal="medals[entry.rank - 1]" :size="26"
        /></span>
      </div>
      <strong class="person-name">{{ entry.name }}</strong
      ><span class="person-score">{{ entry.score }} 积分</span>
      <div class="podium-block">
        <span>{{ entry.rank }}</span
        ><span class="podium-label">{{
          entry.rank === 1
            ? "闪耀之星"
            : entry.rank === 2
              ? "探索达人"
              : "成长先锋"
        }}</span
        ><span class="block-star">✦</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.podium {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 10px;
  padding: 43px 18px 0;
  max-width: 670px;
  margin: 0 auto;
}
.podium-person {
  flex: 1;
  max-width: 170px;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  min-width: 0;
}
.avatar-wrap {
  position: relative;
  margin-bottom: 14px;
}
.person-avatar {
  position: relative;
  width: 59px;
  height: 59px;
  background: linear-gradient(145deg, #ebe0ff, #d7ceff);
  color: #9c85d0;
  border: 4px solid white;
  box-shadow: 0 0 0 2px #cfbdf3;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 22px;
  overflow: hidden;
}
.avatar-star {
  position: absolute;
  color: #fff9;
  font-size: 52px;
  right: -10px;
  bottom: -18px;
}
.place-1 .person-avatar {
  width: 72px;
  height: 72px;
  font-size: 27px;
  background: linear-gradient(145deg, #ffefbf, #ffcc7c);
  color: #d89b33;
  box-shadow: 0 0 0 2px #f2d28f;
}
.place-2 .person-avatar {
  background: linear-gradient(145deg, #ffe0d6, #ffc3bd);
  color: #d68e92;
  box-shadow: 0 0 0 2px #f6c5ba;
}
.person-medal {
  position: absolute;
  bottom: -10px;
  right: -7px;
}
.crown {
  position: absolute;
  z-index: 1;
  color: #f1b747;
  font-size: 37px;
  left: 50%;
  top: -36px;
  transform: translateX(-50%) rotate(-8deg);
}
.person-name {
  display: block;
  font-size: 12px;
  color: #657093;
  width: 100%;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 7px;
}
.place-1 .person-name {
  color: #c59951;
}
.person-score {
  font-size: 11px;
  color: #a0a8bf;
  margin-bottom: 17px;
}
.podium-block {
  width: 100%;
  min-height: 108px;
  border-radius: 13px 13px 0 0;
  background: linear-gradient(160deg, #ded6ff, #b6a6ed);
  display: flex;
  position: relative;
  overflow: hidden;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  color: white;
  gap: 2px;
  box-shadow: inset 0 2px 0 #ffffff70;
}
.place-1 .podium-block {
  min-height: 147px;
  background: linear-gradient(150deg, #ffdb8c, #ffbc58);
}
.place-2 .podium-block {
  min-height: 119px;
  background: linear-gradient(150deg, #ffc0b2, #f59392);
}
.podium-block > span:first-child {
  font-size: 40px;
  font-weight: 750;
  line-height: 1;
  text-shadow: 0 2px 0 #ffffff30;
}
.podium-label {
  font-size: 10px;
  margin-top: 8px;
  letter-spacing: 1px;
  opacity: 0.9;
}
.block-star {
  position: absolute;
  font-size: 93px;
  color: #ffffff17;
  right: -15px;
  bottom: -15px;
  transform: rotate(18deg);
}
@media (max-width: 500px) {
  .podium {
    gap: 8px;
    padding: 43px 2px 0;
  }
  .person-name {
    font-size: 11px;
  }
  .person-score {
    font-size: 10px;
  }
  .person-avatar {
    width: 50px;
    height: 50px;
    font-size: 20px;
  }
  .place-1 .person-avatar {
    width: 64px;
    height: 64px;
    font-size: 24px;
  }
  .podium-block {
    min-height: 94px;
  }
  .place-1 .podium-block {
    min-height: 131px;
  }
  .place-2 .podium-block {
    min-height: 109px;
  }
  .podium-block > span:first-child {
    font-size: 35px;
  }
}
</style>
