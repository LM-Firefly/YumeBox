#pragma once

#include <jni.h>
#include <memory>
#include <string_view>
#include <cstdint>

namespace jni {

class ScopedJString {
public:
    ScopedJString() noexcept = default;
    explicit ScopedJString(JNIEnv* env, jstring str);

    ScopedJString(const ScopedJString&) = delete;
    ScopedJString& operator=(const ScopedJString&) = delete;
    ScopedJString(ScopedJString&& other) noexcept
        : data_(std::move(other.data_)), length_(other.length_) { other.length_ = 0; }
    ScopedJString& operator=(ScopedJString&& other) noexcept {
        if (this != &other) { data_ = std::move(other.data_); length_ = other.length_; other.length_ = 0; }
        return *this;
    }

    [[nodiscard]] const char* c_str() const noexcept { return data_ ? data_.get() : ""; }
    [[nodiscard]] std::string_view view() const noexcept { return data_ ? std::string_view(data_.get(), length_) : std::string_view(); }
    [[nodiscard]] size_t size() const noexcept { return length_; }
    [[nodiscard]] bool empty() const noexcept { return length_ == 0; }
    [[nodiscard]] explicit operator bool() const noexcept { return data_ != nullptr; }
    [[nodiscard]] operator std::string_view() const noexcept { return view(); }

private:
    std::unique_ptr<char[]> data_;
    size_t length_ = 0;
};

class ScopedGlobalRef {
public:
    ScopedGlobalRef() noexcept = default;
    ScopedGlobalRef(JNIEnv* env, jobject obj);

    ScopedGlobalRef(const ScopedGlobalRef&) = delete;
    ScopedGlobalRef& operator=(const ScopedGlobalRef&) = delete;
    ScopedGlobalRef(ScopedGlobalRef&& other) noexcept : env_(other.env_), ref_(other.ref_) {
        other.env_ = nullptr; other.ref_ = nullptr;
    }
    ScopedGlobalRef& operator=(ScopedGlobalRef&& other) noexcept {
        if (this != &other) { reset(); env_ = other.env_; ref_ = other.ref_; other.env_ = nullptr; other.ref_ = nullptr; }
        return *this;
    }
    ~ScopedGlobalRef() { reset(); }

    [[nodiscard]] jobject get() const noexcept { return ref_; }
    [[nodiscard]] explicit operator bool() const noexcept { return ref_ != nullptr; }
    void reset() noexcept;

private:
    JNIEnv* env_ = nullptr;
    jobject ref_ = nullptr;
};

class ScopedThreadAttachment {
public:
    explicit ScopedThreadAttachment(JavaVM* vm);
    ScopedThreadAttachment(const ScopedThreadAttachment&) = delete;
    ScopedThreadAttachment& operator=(const ScopedThreadAttachment&) = delete;
    ~ScopedThreadAttachment();

    [[nodiscard]] JNIEnv* env() const noexcept { return env_; }
    [[nodiscard]] explicit operator bool() const noexcept { return env_ != nullptr; }

private:
    JavaVM* vm_ = nullptr;
    JNIEnv* env_ = nullptr;
    bool attached_ = false;
};

[[nodiscard]] JavaVM* get_global_vm() noexcept;
void initialize_global_vm(JavaVM* vm) noexcept;

} // namespace jni