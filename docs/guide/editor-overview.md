---
outline: deep
---
# 概述
Sora Editor是一个用于编辑代码的Android View库，具有即时语法高亮显示和实时自动补全支持。

该编辑器旨在高效、简洁且可扩展，您可以通过将我们的库集成到您的项目中来轻松在应用程序中实现代码编辑。

<div class="tip custom-block" style="padding-top: 8px">

现在就想试试？立刻前往[快速开始](./getting-started.md)。

</div>

## 编辑器的亮点

### **功能完善**

Sora Editor实现了现代IDE的大多数功能。我们不仅提供基本功能，还额外提供了最前沿的新功能。

目前已实现的功能：
  - 语法高亮
  - 自动补全(包含对[代码块（Code Snippets）](https://macromates.com/manual/en/snippets)的支持)
  - 代码块指示器
  - 无限制文本撤回/重做和快速文本搜索/替换
  - 自动换行
  - 显示不可打印字符
  - 诊断标记和工具提示窗口
  - 文本放大镜
  - 标点符号对匹配和突出显示
  - 粘性滚动
  - 由[TextMate](https://github.com/eclipse/tm4e)和[TreeSitter](https://github.com/AndroidIDEOfficial/android-tree-sitter/)提供语言支持

### **流畅的用户体验**

Sora Editor使用合适的动画来实现组件的过渡，带来流畅的用户体验（UX）。同时组件还具有PC导航模式和快捷键，以供使用鼠标和实体键盘编辑文本的用户使用组件。

### **高性能**

Sora Editor得益于现代的多核处理器，支持在后台工作线程中以增量方式进行特定语言的代码分析。自动补全待选项按需计算并发送至用户界面进行显示。

同时渲染过程也得到了优化。编辑器对可见文本构建测量缓存和[RenderNode](https://developer.android.com/reference/android/graphics/RenderNode)，使得编辑器能够快速重新渲染并响应用户的交互。渲染过程使用的大多数对象都会被重用。

## 讨论

我们创建了用于讨论项目的官方群
- QQ群: [216632648](https://jq.qq.com/?_wv=1027&k=n68uxQws)
- [Telegram群组](https://t.me/rosemoe_code_editor)