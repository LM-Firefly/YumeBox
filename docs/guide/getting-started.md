---
outline: deep
---
# 快速开始
## 要求
您的项目在引入[sora-editor](https://github.com/Rosemoe/sora-editor)库之前，请确保您的构建环境及配置满足以下要求：
* 使用Gradle编译且JDK版本不低于17
* 您模块的最低Android SDK版本至少为Android L（API 21）
  * 如果您需要使用[语言服务器协议](https://microsoft.github.io/language-server-protocol/)，则要求至少为Android O（API 26）
* 项目的编译兼容性和目标兼容性应是`JavaVersion.VERSION_17`
::: details 设置Java源代码兼容性和目标兼容性

::: code-group

```Kotlin{3-4,8-10} [Kotlin DSL]
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
// 如果您的应用使用Kotlin，请额外添加以下配置
// kotlin {
//     jvmToolchain(17)
// }
```

```Groovy{3-4} [Groovy DSL]
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}
```

:::


::: details 使用非Gradle进行构建

编辑器的相关资源通过AAR文件进行分发，所以构建系统必须支持处理AAR文件。

如果您一定要使用非Gradle作为您项目的构建系统，我们将不会为您的构建问题提供任何帮助。

:::
## 添加依赖

最新版本: [![Maven Central](https://img.shields.io/maven-central/v/io.github.rosemoe/editor.svg?label=Maven%20Central)]((https://search.maven.org/search?q=io.github.rosemoe%20editor))

添加sora-editor到您应用的依赖中:

::: code-group

```Kotlin{2-3} [Kotlin DSL]
dependencies {
    implementation(platform("io.github.rosemoe:editor-bom:<版本名>"))
    implementation("io.github.rosemoe:<模块名>")
}
```

```Groovy{2-3} [Groovy DSL]
dependencies {
    implementation(platform("io.github.rosemoe:editor-bom:<版本名>"))
    implementation 'io.github.rosemoe:<模块名>'
}
```

:::

请将`<版本名>`和`<模块名>`替换为正确的版本名称和模块名称。你可以添加多个模块到您的项目中。

以下是一个在编辑器中使用TextMate语法高亮的示例，请根据您的实际情况引入：

::: code-group

```Kotlin{2-4} [Kotlin DSL]
dependencies {
    implementation(platform("io.github.rosemoe:editor-bom:0.24.4"))
    implementation("io.github.rosemoe:editor")
    implementation("io.github.rosemoe:language-textmate")
}
```

```Groovy{2-4} [Groovy DSL]
dependencies {
    implementation(platform("io.github.rosemoe:editor-bom:0.24.4"))
    implementation 'io.github.rosemoe:editor'
    implementation 'io.github.rosemoe:language-textmate'
}
```

```Kotlin{2-4} [Kotlin DSL(不使用bom)]
dependencies {
    val editorVersion = "0.24.4"
    implementation("io.github.rosemoe:editor:$editorVersion")
    implementation("io.github.rosemoe:language-textmate:$editorVersion")
}
```

```Groovy{2-4} [Groovy DSL(不使用bom)]
dependencies {
    def editorVersion = '0.24.4'
    implementation 'io.github.rosemoe:editor:$editorVersion'
    implementation 'io.github.rosemoe:language-textmate:$editorVersion'
}
```

:::

::: tip 注意

您可以通过上面的徽章中得知最新的版本名称，也可以前往我们的GitHub [Releases](https://github.com/Rosemoe/sora-editor/releases)页面获取完整的版本列表。

当前可供使用的模块有： `editor`、`editor-lsp`、`language-java`, `language-textmate`以及`language-treesitter`。
请参考下面的表格获取对应模块的相关信息。

:::

### 🛠️可用模块

| 模块                | 介绍                                                                                                                                                                                                                                                                                                                     |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| editor              | 包含编辑器的核心框架。                                                                                                                                                                                                                                                                                                   |
| editor-lsp          | 可以使用语言服务器协议（简称LSP）创建语言的便捷工具库。                                                                                                                                                                                                                                                                  |
| language-java       | 包含Java高亮和自动补全的语言库。                                                                                                                                                                                                                                                                                         |
| language-textmate   | 一个高级的高亮分析库。你可以借助它来加载textmate语言配置文件并应用于本编辑器。 内部实现来自[tm4e](https://github.com/eclipse/tm4e)。                                                                                                                                                                                     |
| language-treesitter | 为编辑器提供[tree-sitter](https://tree-sitter.github.io/tree-sitter/)支持。tree-sitter可用于快速、增量地将代码转换 成抽象语法树，以便您向用户提供精确的高亮和自动补全功能。注意此模块仅提供了转换和高亮支持。感谢[android-tree-sitter](https://github.com/AndroidIDEOfficial/android-tree-sitter/)项目提供的Java绑定库。 |

| oniguruma-native | 用于 TextMate 的正则表达式库。默认不使用。 |

### 🚧快照构建

通常情况下我们建议使用[正式发布的版本](https://github.com/Rosemoe/sora-editor/releases)。但有时候您可能需要使用最新的构建版本以取得最新的错误修复和功能更新。

::: details 如何使用快照构建

快照版本在存储库推送时自动发布。您可以将当前发布的版本名称和 `-SNAPSHOT` 组合在一起，得到快照版本名称。

举个例子, 如果目前最新正式发布的版本是 `0.24.1`，则可以将 `0.24.1-SNAPSHOT` 作为版本号导入快照版本到您的项目中。

需要注意的是，使用快照版本您需要额外添加一个maven存储库：
```Kotlin{3}
repositories {
    // ...
    maven("https://central.sonatype.com/repository/maven-snapshots")
}
```

由于 Central Portal 的限制，快照版本仅会保留 90 天。

:::

## 为TextMate配置脱糖

如果您的项目使用了`language-textmate`模块，并且想要在Android 13（API 33）以下的设备上运行您的应用，您**必须**启用[脱糖](https://developer.android.google.cn/studio/write/java8-support#library-desugaring)以避免兼容性问题。如果您已进行此操作，请看下一部分。

如果要启用脱糖，请按照以下说明配置您的**应用模块**。

* 添加脱糖依赖
::: code-group

```Kotlin [Kotlin DSL]
dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5") // [!code highlight]
}
```

```Groovy [Groovy DSL]
dependencies {
    coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:2.1.5' // [!code highlight]
}
```

:::

* 添加编译选项
::: code-group

```Kotlin [Kotlin DSL]
android {
    compileOptions {
        isCoreLibraryDesugaringEnabled = true // [!code highlight]
    }
}
```

```Groovy [Groovy DSL]
android {
    compileOptions {
        coreLibraryDesugaringEnabled true // [!code highlight]
    }
}
```

:::

::: warning 注意
当您启用脱糖时，您应该通过如下两种方式构建用于发布的应用：
* 菜单  `Build` | `Build Bundle(s) / APK(s)` | `Build APK(s)` 进行构建
* 或者，运行Gradle任务 `assemble<Variant>`。 例如，运行 `assembleDebug` 任务来生成`debug`变体的 APK

当您从运行按钮（或快捷键 `Shift+F10`）在特定设备上运行应用时，Android Studio 尝试加速解糖过程。它将生成目标设备 API 特定的 APK 文件，此安装包可能不能在其他设备上正常工作。

或者，您可以禁用 Android Studio 的这项功能来解决此问题。此项设置位于 `Experimental > Optimize build for target device API level only` 。
:::

## 创建组件

请确保您的项目中已经包含核心模块`editor`，并且您项目的Gradle相关文件已经成功同步。

主要的widget类名是`io.github.rosemoe.sora.widget.CodeEditor`。您可以通过XML或Java/Kotlin代码（推荐）创建代码编辑器，但是在XML中只能设置有限的属性。

### 在XML使用

在布局XML文件中声明编辑器：

```XML
<io.github.rosemoe.sora.widget.CodeEditor
    android:id="@+id/editor"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:text="Hello, world!"
    app:textSize="18sp" />
```

无需在XML的声明中设置`text`或者`textSize`。

有关其在XML中的用法，请参考[XML属性](/reference/xml-attributes)。

::: tip 注意
不建议编辑器宽度或高度是`wrap_content`。因为在这种情况下编辑文本时，编辑器会请求布局重新绘制，可能会导致性能问题或者卡顿。
:::

### 使用Java/Kotlin代码

如果我们处于`Activity`上下文或者`ViewGroup`中，只需要实例化一个编辑器对象并将其添加到任意的视图组中即可。

::: code-group

```Kotlin [Kotlin]
val editor = CodeEditor(this)
editor.setText("Hello, world!") // 设置文本
editor.typefaceText = Typeface.MONOSPACE // 使用Monospace字体
editor.nonPrintablePaintingFlags =
                CodeEditor.FLAG_DRAW_WHITESPACE_LEADING or CodeEditor.FLAG_DRAW_LINE_SEPARATOR or CodeEditor.FLAG_DRAW_WHITESPACE_IN_SELECTION // Show Non-Printable Characters
vg.add(editor, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
```

```Java [Java]
var editor = new CodeEditor(this);
editor.setText("Hello, world!"); // 设置文本
editor.setTypefaceText(Typeface.MONOSPACE); // 使用Monospace字体
editor.setNonPrintablePaintingFlags(
                CodeEditor.FLAG_DRAW_WHITESPACE_LEADING | CodeEditor.FLAG_DRAW_LINE_SEPARATOR | CodeEditor.FLAG_DRAW_WHITESPACE_IN_SELECTION); // Show Non-Printable Characters
vg.add(editor, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
```

:::
参考`CodeEditor`中声明的方法和`DirectAccessProps`的字段，您可以对编辑器进行更丰富的配置。

::: warning 请谨慎
`DirectAccessProps`的字段并非在任何情况下都是立即生效的。在使用被`@InvalidateRequired`标记的字段后需要您显式的调用编辑器的`invalidate()`。

您不应该使用被`@UnsupportedUserUsage`标记的字段，因为它们只能被内部使用。
:::

## 释放组件
当一个`CodeEditor`实例不再被使用的时候，您应该调用其`release()`方法释放编辑器资源和为编辑器服务的后台线程。同时释放编辑器后不应使用此编辑器，避免出现意外错误。

::: code-group

```Kotlin Kotlin
override fun onDestroy() {
    super.onDestroy()
    editor?.release()
}
```

```Java Java
@Override
protected void onDestroy() {
    super.onDestroy();
    if (editor != null) {
        editor.release();
    }
}
```

:::

## 更进一步
前往[语言](/language.md)和[配色方案](/color-scheme.md)为编辑器提供编程语言支持和自定义配色方案。