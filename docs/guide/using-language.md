---
outline: deep
---

# 语言支持

Language是Sora Editor实现提供特定语言例如语法分析、自动补全和缩进等功能的接口。

单个`Language`实例应当只提供给一个编辑器使用，而且当一个编辑器实例被销毁或者设置新的`Language`实例时，旧的`Language`
将会自动销毁。

你可以调用`CodeEditor#setEditorLanguage`为编辑器设置一个新的`Language`。默认情况下，编辑器使用内置的`EmptyLanguage`
作为自己的语言，并且不进行任何分析，即语法高亮和其他语言功能均不可使用。

我们提供了一些通用的语言功能实现供您设置编程语言的分析和语法高亮显示。但请注意`language-java`模块只适用于简单的Java关键字的语法高亮。

## 使用语言模块

在使用语言模块之前，请确保已将其导入到项目中。

### language-textmate

此模块使用[textmate](https://github.com/textmate/textmate)语法来标记文本并实现各种编程语言的高亮显示。
textmate在[Visual Studio Code](https://github.com/microsoft/vscode)和[Eclipse](https://github.com/eclipse/tm4e)
中也用于语法高亮。大多数情况下请使用这个模块实现语法高亮，而不是自己编写`Language`实现。

请按照以下步骤将textmate用于您的编辑器。

#### 查找语言语法和配置

textmate支持非常多的语言，它的语法高亮规则由名为`*.tmLanguage`的PLIST文件或者名为`*.tmLanguage.json`的JSON文件定义。
你需要textmate规则文件(又叫`语法`)和可选的语言配置文件(`*.language-configuration.json`)为您的语言提供高亮配置。

您可以在以下位置找到这些文件：

* [TM4E语言包](https://github.com/eclipse/tm4e/tree/25e7fbe39c02644ca5d541d20a2c601791af7b8d/org.eclipse.tm4e.language_pack/syntaxes)
* [VSCode扩展](https://github.com/microsoft/vscode/tree/4f2ff19ecacffa0aa4874db4d63ed4e899d98431/extensions)

::: tip 注意

当前的 TextMate 引擎对某些 TextMate 语法文件支持不完全。这是正则表达式库 [Joni](https://github.com/jruby/joni) 对这些语法文件中的正则表达式支持不完全导致的。

在进行高亮分析时，这些不受支持的正则表达式将用 `^$` 代替以避免产生错误。

:::
#### 查找主题

textmate必须和textmate主题搭配使用。你还需要从[VSCode Extensions](https://github.com/microsoft/vscode/tree/4f2ff19ecacffa0aa4874db4d63ed4e899d98431/extensions)
中查找您满意的主题JSON文件。
里面有一些文件夹的命名规则是`theme-*`。他们就是您要找的VSCode内置的textmate主题。

#### 准备语言注册表

textmate可以加载多种语言，所以我们需要提前准备一下`languages.json`。现在假设您的assets文件夹的目录结构如下：

```Text
.
├─ textmate
│  ├─ java
│  │  ├─ syntaxes
│  │  │  └─ java.tmLanguage.json
│  │  └─ language-configuration.json
│  └─ kotlin
│     ├─ syntaxes
│     │  └─ Kotlin.tmLanguage
│     └─ language-configuration.json
└─ language.json
```

您的`language.json`内容如下：

```JSON
{
  "languages": [
    {
      "grammar": "textmate/java/syntaxes/java.tmLanguage.json",
      "name": "java",
      "scopeName": "source.java",
      "languageConfiguration": "textmate/java/language-configuration.json"
    },
    {
      "grammar": "textmate/kotlin/syntaxes/Kotlin.tmLanguage",
      "name": "kotlin",
      "scopeName": "source.kotlin",
      "languageConfiguration": "textmate/kotlin/language-configuration.json"
    }
  ]
}
```

`name`可以自行定义，`scopeName`是语法文件的作用域。

对于可以嵌入其他语言的语言，例如HTML和Markdown等标记语言。请参考适用于HTML的[示例应用](https://github.com/Rosemoe/sora-editor/blob/main/app/src/main/assets/textmate/languages.json)

#### 加载语法和主题

在编辑器中使用textmate之前，我们应该将语法和主题文件提前加载到注册表中。
**无论有多少编辑器实例在使用textmate，这些步骤都应该只执行一次。**

假设我们要从assets文件夹加载textmate文件。首先，我们需要添加textmate内部文件访问权限。

::: code-group

```Kotlin Kotlin
FileProviderRegistry.getInstance().addFileProvider(
    AssetsFileResolver(
        applicationContext.assets // 使用应用上下文
    )
)
```

```Java Java
FileProviderRegistry.getInstance().addFileProvider(
    new AssetsFileResolver(
        getApplicationContext().getAssets() // 使用应用上下文
    )
)
```

:::

然后，我们需要加载主题。下面的代码演示如何将单个主题加载到编辑器中。

::: code-group

```Kotlin Kotlin
val themeRegistry = ThemeRegistry.getInstance()
val name = "quietlight" // 主题名称
val themeAssetsPath = "textmate/$name.json"
themeRegistry.loadTheme(
    ThemeModel(
        IThemeSource.fromInputStream(
            FileProviderRegistry.getInstance().tryGetInputStream(themeAssetsPath), themeAssetsPath, null
        ),
        name
    ).apply {
        // 如果主题是适用于暗色模式的，请额外添加以下内容
        // isDark = true
    }
)
```

```Java Java
var themeRegistry = ThemeRegistry.getInstance();
var name = "quietlight"; // 主题名称
var themeAssetsPath = "textmate/" + name + ".json";
var model = new ThemeModel(
        IThemeSource.fromInputStream(
                FileProviderRegistry.getInstance().tryGetInputStream(themeAssetsPath), themeAssetsPath, null
        ),
        name
);
// 如果主题是适用于暗色模式的，请额外添加以下内容
// model.setDark(true);
themeRegistry.loadTheme(model);
```

:::

接下来，为textmate选择并启用一个主题。textmate将会使用其注册表来管理全局配色方案。

::: code-group

```Kotlin Kotlin
ThemeRegistry.getInstance().setTheme("您的主题名称")
```

```Java Java
ThemeRegistry.getInstance().setTheme("您的主题名称");
```

:::

最后，我们加载语言的语法和配置。

::: code-group

```Kotlin Kotlin
GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
```

```Java Java
GrammarRegistry.getInstance().loadGrammars("textmate/languages.json");
```

:::

::: details 通过Kotlin DSL语法加载

您可以使用Kotlin DSL将Language加载到语法注册表中，而不需要`languages.json`。

示例：

```Kotlin
GrammarRegistry.getInstance().loadGrammars(
    languages {
        language("java") {
            grammar = "textmate/java/syntaxes/java.tmLanguage.json"
            defaultScopeName()
            languageConfiguration = "textmate/java/language-configuration.json"
        }
        language("kotlin") {
            grammar = "textmate/kotlin/syntaxes/Kotlin.tmLanguage"
            defaultScopeName()
            languageConfiguration = "textmate/kotlin/language-configuration.json"
        }
        language("python") {
            grammar = "textmate/python/syntaxes/python.tmLanguage.json"
            defaultScopeName()
            languageConfiguration = "textmate/python/language-configuration.json"
        }
    }
)
```

`defaultScopeName()`会将`scopeName`设置为`source.${languageName}`。

:::

#### Optional Step

TextMate 默认情况下使用 Joni 作为正则表达式库。Joni 是由纯 Java 实现的，因此在正则表达式匹配过程中，将会生成大量的对象，给虚拟机带来 GC 压力。

为此，我们在 `0.24.0` 版本中引入了原生的 [oniguruma](https://github.com/kkos/oniguruma) 实现，您可以引入 `oniguruma-native` 模块以启用它。这能够加速 TextMate 的高亮速度并减少内存分配。

oniguruma 也是 VSCode 使用的正则表达式库，因此在引入它之后，您应该能使用更多来自 VSCode 的 TextMate Bundles.

我们的[oniguruma 分支](https://github.com/project-sora/oniguruma) 目前支持 Unicode 17.0.0.

#### 设置编辑器

设置编辑器的配色方案。如果`TextMateColorScheme`没被应用到编辑器中，则textmate的语法高亮结果的颜色是透明的。

::: code-group

```Kotlin Kotlin
editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
```

```Java Java
editor.setColorScheme(TextMateColorScheme.create(ThemeRegistry.getInstance()));
```

:::

最后，设置编辑器语言。

::: code-group

```Kotlin Kotlin
val languageScopeName = "source.java" // 您目标语言的作用域名称
val language = TextMateLanguage.create(
    languageScopeName, true /* true表示启用自动补全 */
)
editor.setEditorLanguage(language)
```

```Java Java
var languageScopeName = "source.java"; // 您目标语言的作用域名称
var language = TextMateLanguage.create(
        languageScopeName, true /* true表示启用自动补全 */
);
editor.setEditorLanguage(language);
```

:::

恭喜！您已经完成了所有设置。尽情享受吧！

### language-java

为Java语言提供基于token的高亮显示、关键字自动补全和代码块标记的支持。它还具有一些用于测试编辑器的实验性功能。

虽然它的功能仍然很简单，但它的速度要比其他复杂的语言分析快得多。

若要创建并使用它，请参考下面的代码：

::: code-group

```Kotlin Kotlin
editor.editorLanguage = JavaLanguage()
```

```Java Java
editor.setEditorLanguage(new JavaLanguage());
```

:::

### language-treesitter

TreeSitter由[Atom](https://github.com/atom/atom)和现在[Zed](https://github.com/zed-industries/zed)的作者们开发。
TreeSitter是一个解析器生成器工具和一个增量解析库。

使用TreeSitter，我们可以为源文件构建抽象语法树，并在编辑源文件时高效地更新该语法树并使用语法树进行准确的语法高亮显示。

我们使用[android-tree-sitter](https://github.com/AndroidIDEOfficial/android-tree-sitter)调用tree-sitter的API。

在继续往下阅读前，我们强烈建议您先了解一下编辑器框架中的[TextStyle](https://github.com/Rosemoe/sora-editor/blob/main/editor/src/main/java/io/github/rosemoe/sora/lang/styling/TextStyle.java)

#### 准备语言

您可以先在[android-tree-sitter](https://github.com/AndroidIDEOfficial/android-tree-sitter)
中查找是否已存在您需要的语言实现。如果没有您需要的语言实现，则您必须自己构建一个适用于Android的语言实现。

此外，还需要四个`scm`文件来查询语法树：

*
    1. 高亮显示

对于适用于绝大多数语言的`highlights.scm`都可以在TreeSitter语言存储库中找到。
例如[这个](https://github.com/tree-sitter/tree-sitter-java/tree/master/queries)适用于Java语言。

*
    2. 代码块（可选）

这个仅适用于sora-editor。
请参考[这个](https://github.com/Rosemoe/sora-editor/blob/main/app/src/main/assets/tree-sitter-queries/java/blocks.scm)示例

*
    3. 符号对高亮（可选）

这个仅适用于sora-editor。
请参考[这个](https://github.com/Rosemoe/sora-editor/blob/main/app/src/main/assets/tree-sitter-queries/java/brackets.scm)
示例

*
    4. 局部变量（可选）

对于适用于大多数语言局部变量的`locals.scm`
可以在[nvim-treesitter](https://github.com/nvim-treesitter/nvim-treesitter/tree/master/queries)存储库中找到。

相关链接：

* [TreeSitter组织](https://github.com/tree-sitter)
* [TreeSitter文档](https://tree-sitter.github.io/tree-sitter/)
* [nvim-treesitter](https://github.com/nvim-treesitter/nvim-treesitter)
* [Zed Languages](https://github.com/zed-industries/zed/tree/main/crates/zed/src/languages)

#### 创建语言详细说明

首先，`TsLanguageSpec`应该由tree-sitter语言实例和`scm`源文本创建。您可能需要为您的`locals.scm`添加一个自定义的[`LocalsCaptureSpec`](https://github.com/Rosemoe/sora-editor/blob/main/language-treesitter/src/main/java/io/github/rosemoe/sora/editor/ts/LocalsCaptureSpec.kt)。

```Kotlin
val spec = TsLanguageSpec(
    // 您的tree-sitter语言实例
    language = TSLanguageJava.getInstance(),
    // scm原文本
    highlightScmSource = assets.open("tree-sitter-queries/java/highlights.scm")
        .reader().readText(),
    codeBlocksScmSource = assets.open("tree-sitter-queries/java/blocks.scm")
        .reader().readText(),
    bracketsScmSource = assets.open("tree-sitter-queries/java/brackets.scm")
        .reader().readText(),
    localsScmSource = assets.open("tree-sitter-queries/java/locals.scm")
        .reader().readText(),
    localsCaptureSpec = object : LocalsCaptureSpec() {
        // 覆盖和更改任何语言详细说明的方法
    }
)
```

有时，您的`scm`文件使用外部谓词方法（客户端谓词）来更好地查询语法树。在这种情况下，请将谓词实现添加到`predicates`参数中。

#### 制作Language和主题

使用您的`TsLanguageSpec`和主题构建器DSL语法创建一个`TsLanguage`

```Kotlin
// 在Kotlin中轻松制作文本样式的扩展功能
import io.github.rosemoe.sora.lang.styling.textStyle

// ...
val language = TsLanguage(languageSpec, false /* useTab */) {
    // 主题构建器DSL
    // 将文本样式应用于捕获的语法节点

    // 将样式应用于单一类型的节点
    textStyle(KEYWORD, bold = true) applyTo "keyword"
    // 应用于多个节点
    textStyle(LITERAL) applyTo arrayOf("constant.builtin", "string", "number")
}
```

#### 应用Language

现在，Language实例可以应用于编辑器。

```Kotlin
editor.setEditorLanguage(language)
```

请注意，`TsLanguageSpec`对象不可重复使用，因为`TsLanguage`被销毁时它也会被关闭。