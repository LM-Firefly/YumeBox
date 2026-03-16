---
outline: deep
---

# CodeEditor in Compose

Jetpack Compose是用于Android开发的新框架。如果您尝试使用Sora Editor，同时使用使用Jetpack Compose构建的应用程序。本文档可能会对您有所帮助。

> 我的英语可能不好，如果有的话请纠正它们。谢谢。

## 创建状态持有者

首先，我们将定义一个`CodeEditorState`，封装并持有`CodeEditor`的状态.

```kotlin
data class CodeEditorState(
    val editor: CodeEditor? = null,
    val initialContent: Content = Content()
) {
    var content by mutableStateOf(initialContent)
}
```

您可以根据需要添加许多状态。

::: tip 注意
如果您不想使用`ViewModel`而是希望使用`remember*()`可组合函数，您可以进行以下操作：

```kotlin
@Composable
fun rememberCodeEditorState(
    initialContent: Content = Content()
) = remember {
    CodeEditorState(
        initialContent = initialContent
    )
}
```
:::

## 创建`CodeEditor`可组合项

现在，我们将创建`CodeEditor`可组合项，它通过`AndroidView`间接地实现可组合化。在这个可组合项中，它将接受一个与`CodeEditorState`类型的参数`state`.

```kotlin
@Composable
fun CodeEditor(
    modifier: Modifier = Modifier,
    state: CodeEditorState
) {
    // ...
}
```

### 设置`CodeEditor`工厂

我们需要一个`Context`以实例化一个`CodeEditor`。

```kotlin
private fun setCodeEditorFactory(
    context: Context,
    state: CodeEditorState
): CodeEditor {
    val editor = CodeEditor(context)
    editor.apply {
        setText(state.content)
        // ...
    }
    state.editor = editor
    return editor
}
```

一旦我们完成了工厂的创建，我们现在就可以用`remember`可组合函数进行管理.

```kotlin
@Composable
fun CodeEditor(
    modifier: Modifier = Modifier,
    state: CodeEditorState
) {
    val context = LocalContext.current
    val editor = remember {
        setCodeEditorFactory(
            context = context,
            state = state
        )
    }
    AndroidView(
        factory = { editor },
        modifier = modifier,
        onRelease = {
            it.release()
        }
    )
    // ...
}
```

### 为`CodeEditor`的状态设置`LaunchedEffect`

当`CodeEditor`的状态发生变化时执行某些代码，我们需要使用`LaunchedEffect`

```kotlin
LaunchedEffect(key1 = state.content) {
    state.editor?.setText(state.content)
}
```

## 使用`CodeEditor`可组合项

在我们完成`CodeEditor`可组合项后，我们就可以在应用程序中使用它。首先，**强烈建议**在`ViewModel`中定义`CodeEditorState`。

例如，在`MainScreen`中，我们将创建一个`MainViewModel`，在这个viewmodel中，我们定义了一个`CodeEditorState`。

```kotlin
class MainViewModel : ViewModel() {
    val editorState by mutableStateOf(
        CodeEditorState()
    )
}
```

接着在`MainScreen`可组合项中，使用`Modifier`对您的`CodeEditor`进行必要的调整。

```kotlin
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    Column {
        CodeEditor(
            modifier = Modifier
                .fillMaxSize(),
            state = viewModel.editorState
        )
    }
}
```

::: warning 警告
如果`CodeEditor`的底部有可组合项，请使用`Modifier.weight(1f)`而不是`Modifier.fillMaxSize()`。否则`CodeEditor`将会占满整个容器。
:::

## 结语

一般情况下，`CodeEditor`是我们唯一需要使用`AndroidView`的小部件。至于其他类似于`SymbolInputView`的组件，目前建议完全使用Compose进行实现。

---

以上便是全部。我并不知道在JetPack Compose中使用Sora编辑器的`CodeEditor`是否是一个正确的选择，但我希望您能通过本指南获得灵感。感谢您的阅读。