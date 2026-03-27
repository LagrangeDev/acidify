# 在 Node.js 中使用

## 引入依赖

[![npm](https://img.shields.io/npm/v/%40acidify%2Fcore)](https://www.npmjs.com/package/@acidify/core)

Acidify 支持导出到 Kotlin/JS 平台，除了在 Maven Central 发布了 `acidify-core-js` 库之外，还在 NPM Registry 发布了供纯 JavaScript 开发者使用的 `@acidify/core`，包含了 Acidify 的核心功能的 JavaScript 实现，可以在 Node.js 16+ 环境中使用。可以通过以下命令在项目中引入：

```bash
npm install @acidify/core
```

`@acidify/core` 包含完整的 TypeScript 类型定义，因此可以在 TypeScript 项目中使用。

## 使用示例

`@acidify/core` 包导出的类型和 API 与 Kotlin 版本的 `acidify-core` 基本保持一致，但在其他方面有一些重要的区别：

- 创建一个 `Bot` 或 `AndroidBot` 实例时，需要使用 `Bot.create()` 或 `AndroidBot.create()` 静态方法来创建实例，而不是直接调用构造函数。
- 在使用 Kotlin 单例对象时（如 `SimpleLogHandler`），需要通过 `getInstance()` 方法来获取实例。
- 由于 `kotlinx-coroutines` 并没有针对 JavaScript 平台提供类型定义，因此 `@acidify/core` 提供了一个简化的 `CoroutineScope` 包装类型，接收一个 `boolean` 表示该协程作用域是否为 `SupervisorJob`，同时只暴露了一个方法 `cancel()` 用于取消协程作用域。
- `Bot` 和 `AndroidBot` 没有暴露 `eventFlow` 属性，而是转而通过 `onXxx` 方法来注册事件处理器，同时提供了对应的 `offXxx` 方法来注销事件处理器。

以下是一个使用 TypeScript 代码用 PC 协议登录的示例：

```typescript
// 从文件中加载 SessionStore，如果文件不存在则创建一个空的 SessionStore
let sessionStore: SessionStore;
if (existsSync('session-store.json')) {
  const data = await readFile('session-store.json', 'utf-8');
  sessionStore = SessionStore.fromJson(data);
} else {
  sessionStore = SessionStore.empty();
}

const scope = new CoroutineScope(true);

// 创建一个 UrlSignProvider 实例，参数为签名服务的 URL
// 如果要对接新版 Lagrange V2 Sign API，请使用 LagrangeUrlSignProvider
const signProvider = new UrlSignProvider(scope, '...');

// 创建 Bot 实例
const bot = Bot.create(
  await signProvider.getAppInfo()!,
  sessionStore,
  signProvider,
  scope,
  LogLevel.DEBUG,
  SimpleLogHandler.getInstance()
);

bot.onSessionStoreUpdated(async (event) => {
  await writeFile('session-store.json', event.sessionStore.toJson());
});
await bot.login();

await bot.sendGroupMessage(111111111n, async (b) => {
  b.text('Hello, @acidify/core from Node.js!');
});

await bot.offline();
scope.cancel(); // 停止所有的协程，结束程序
```

更多的使用说明请参考[在 Kotlin 中使用](./kotlin.md)。
