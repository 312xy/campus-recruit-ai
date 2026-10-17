<script setup>
// 根组件：只负责放一个"路由出口"。
// 人话讲解：<router-view /> 是个占位符 —— 当前 URL 匹配到哪个页面组件，就把它渲染在这里。
// 所以真正的页面内容都在 views/ 目录下的组件里，App.vue 保持干净。
</script>

<template>
  <router-view />
</template>

<style>
/* 全局基础样式：让页面的 body 撑满、去掉默认外边距 */
html,
body,
#app {
  height: 100%;
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

/* 移动端全局防护（放这里而不是 scoped 样式里）：
   1) overflow-x: hidden —— 只要有一个元素算宽了 4px，整页就能左右拖，
      手机上这体验非常糟糕。这里兜底裁掉横向溢出。
   2) 文本自动换行 —— 长订单号/长 URL 不换行会把容器顶宽。
   3) tap-highlight —— 去掉 iOS/安卓点按时的蓝色高亮方块。 */
@media (max-width: 768px) {
  html,
  body {
    overflow-x: hidden;
    -webkit-text-size-adjust: 100%;
  }
  body {
    -webkit-tap-highlight-color: transparent;
    overflow-wrap: break-word;
    word-break: break-word;
  }
}
</style>
