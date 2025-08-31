#pragma once

#include <jni.h>
#include "jni_raii.hpp"
#include "trace.h"

namespace jni {

[[nodiscard]] inline jstring newString(JNIEnv* env, std::string_view str) {
    if (!env) return nullptr;
    return env->NewStringUTF(str.empty() ? "" : str.data());
}

template<typename F>
    requires std::is_convertible_v<std::invoke_result_t<F>, std::string_view>
[[nodiscard]] jstring query(JNIEnv* env, F&& func) {
    TRACE_METHOD();
    if (!env) return nullptr;
    return newString(env, std::forward<F>(func)());
}

template<typename F>
[[nodiscard]] jstring queryNullable(JNIEnv* env, F&& func) {
    TRACE_METHOD();
    if (!env) return nullptr;
    auto result = std::forward<F>(func)();
    if (result.empty() || result.data() == nullptr) return nullptr;
    return newString(env, result);
}

template<typename F>
void voidOp(JNIEnv* env, jstring p, F&& func) {
    TRACE_METHOD();
    if (!env) return;
    ScopedJString str(env, p);
    std::forward<F>(func)(str.view());
}

template<typename F>
void voidOp(JNIEnv* env, jstring p1, jstring p2, F&& func) {
    TRACE_METHOD();
    if (!env) return;
    ScopedJString s1(env, p1), s2(env, p2);
    std::forward<F>(func)(s1.view(), s2.view());
}

template<typename F>
[[nodiscard]] jboolean boolOp(JNIEnv* env, jstring p1, jstring p2, F&& func) {
    TRACE_METHOD();
    if (!env) return JNI_FALSE;
    ScopedJString s1(env, p1), s2(env, p2);
    return std::forward<F>(func)(s1.view(), s2.view()) ? JNI_TRUE : JNI_FALSE;
}

template<typename F>
void callbackOp(JNIEnv* env, jobject cb, F&& func) {
    TRACE_METHOD();
    if (!env || !cb) return;
    ScopedGlobalRef ref(env, cb);
    std::forward<F>(func)(ref.get());
}

template<typename F>
void callbackOp(JNIEnv* env, jobject cb, jstring p, F&& func) {
    TRACE_METHOD();
    if (!env || !cb) return;
    ScopedGlobalRef ref(env, cb);
    ScopedJString str(env, p);
    std::forward<F>(func)(ref.get(), str.view());
}

template<typename F>
auto withThread(F&& func) -> std::invoke_result_t<F, JNIEnv*> {
    JavaVM* vm = get_global_vm();
    if (!vm) return {};
    ScopedThreadAttachment attach(vm);
    if (!attach) return {};
    return std::forward<F>(func)(attach.env());
}

} // namespace jni