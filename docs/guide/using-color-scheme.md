---
outline: deep
---

# 配色方案

编辑器的配色方案由(`EditorColorScheme`)管理. 

颜色类型由不同的整数 ID 表示。`EditorColorScheme`内部维护颜色类型ID到实际颜色的映射。内置颜色类型应定义为类中静态的整数常量。

与`Language`不同的是，同一个`EditorColorScheme`实例可以为多个编辑器服务，配色方案会通知它所服务的所有编辑器，以更新其渲染结果。您可以使用共享的配色方案实例管理编辑器文本的效果。当调用`CodeEditor#release`的时候，编辑器会和它的配色方案解绑。

编辑器对象在这个类中是`弱引用`的，因此编辑器可以被安全的释放。

## 更新配色方案

通过`CodeEditor#getColorScheme`获取编辑器的配色方案。

使用`EditorColorScheme#setColor(int, int)`将新颜色应用于编辑器。

示例：

::: code-group
```Kotlin
val scheme = editor.colorScheme
scheme.setColor(EditorColorScheme.KEYWORD, Color.RED)
```
```Java
var scheme = editor.getColorScheme();
scheme.setColor(EditorColorScheme.KEYWORD, Color.RED);
```
:::

请注意，多个编辑器可以共享相同的配色方案。更改一个编辑器的配色方案也可能导致其他编辑器的颜色更改。

## 扩展配色方案

有时我们需要扩展类以更好地定义您自己的配色方案。

### 覆盖默认颜色

子类应重写`applyDefault()`以应用其默认颜色，尽管其他方法没有被final修饰。

重写此方法后，您必须调用超类的`applyDefault()`，然后调用一系列setColor(int, int)应用您自己的配色。有时我们会在编辑器库中添加新的颜色类型。如果不调用超类的方法，可能会缺少一些重要的颜色。

### 添加新的颜色类型

您可以将不在预定义池中的颜色ID用于自定义Language。我们建议您为自定义颜色ID添加一个基本的偏移量。例如，第一个自定义颜色类型 ID 是`256`。这可以为编辑器后面的内置类型预留足够的空间并且有效的避免后续的ID冲突。

最后一个预定义的颜色类型ID是`EditorColorScheme.END_COLOR_ID`。

## 默认配色方案

默认情况下，新创建的编辑器使用全局默认配色方案。默认配色方案由这些编辑器共享，可从`EditorColorScheme#getDefault`获取。

您可以通过`EditorColorScheme#setDefault`更新全局默认配色方案。

* 更新全局默认配色方案。新创建的编辑器将使用新的默认配色方案。

::: code-group
```Kotlin
EditorColorScheme.setDefault(MyColorScheme())
```
```Java
EditorColorScheme.setDefault(new MyColorScheme());
```
:::

* 更新全局默认配色方案，并同时应用于使用了旧默认配色方案的编辑器

::: code-group
```Kotlin
EditorColorScheme.setDefault(MyColorScheme(), true)
```
```Java
EditorColorScheme.setDefault(new MyColorScheme(), true);
```
:::