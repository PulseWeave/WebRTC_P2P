# 故障排除指南

## 当前问题

项目遇到了以下构建问题：

1. **Java 版本兼容性**：系统使用 Java 21，但 Gradle 版本不兼容
2. **WebRTC 依赖问题**：无法找到正确的 WebRTC 依赖版本
3. **Gradle Wrapper 缺失**：缺少 gradle-wrapper.jar 文件

## 解决方案

### 方案 1：使用 Android Studio 自动修复（推荐）

1. 在 Android Studio 中打开项目
2. 当提示 "Gradle project sync failed" 时，点击 "Fix Gradle wrapper and re-import project"
3. Android Studio 会自动下载正确的 Gradle 版本和创建必要的文件

### 方案 2：手动修复

#### 步骤 1：下载 Gradle Wrapper
```bash
# 在项目根目录执行
curl -O https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar
mv gradle-wrapper.jar gradle/wrapper/
```

#### 步骤 2：修复 WebRTC 依赖
在 `app/build.gradle` 中，将 WebRTC 依赖替换为：

```gradle
// 使用官方 WebRTC 依赖
implementation 'org.webrtc:google-webrtc:1.0.32006'

// 或者使用 JitPack 版本
implementation 'com.github.webrtc-sdk:android:104.5112.08'
```

#### 步骤 3：添加 JitPack 仓库
在项目级 `build.gradle` 中添加：
```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

### 方案 3：使用系统 Gradle

如果系统已安装 Gradle，可以直接使用：
```bash
gradle build
```

## 常见错误及解决方法

### 错误 1：Java 版本不兼容
```
Your build is currently configured to use incompatible Java 21.0.6 and Gradle 7.6.1
```

**解决方法**：
- 升级 Gradle 到 8.5 或更高版本
- 或者降级 Java 到版本 19 或更低

### 错误 2：WebRTC 依赖找不到
```
Could not find org.webrtc:google-webrtc:1.0.32006
```

**解决方法**：
- 检查依赖版本是否正确
- 确保添加了正确的 Maven 仓库
- 尝试使用不同的 WebRTC 依赖版本

### 错误 3：Gradle Wrapper 缺失
```
Could not open init generic class cache for initialization script
```

**解决方法**：
- 让 Android Studio 自动修复
- 或者手动下载 gradle-wrapper.jar 文件

## 推荐的开发环境

- **Java**: 版本 17 或 19
- **Gradle**: 版本 8.5 或更高
- **Android Gradle Plugin**: 版本 8.1.4 或更高
- **Android Studio**: 最新版本

## 下一步

1. 使用 Android Studio 打开项目
2. 让 Android Studio 自动修复 Gradle 问题
3. 项目同步成功后，取消注释 WebRTC 相关代码
4. 测试构建和运行

## 注意事项

- 首次构建可能需要较长时间下载依赖
- 确保网络连接正常，能够访问 Google Maven 仓库
- 如果仍有问题，可以尝试清理项目：`./gradlew clean` 