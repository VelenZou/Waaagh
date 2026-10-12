# Call Hierarchy 递归回归用例

用于验证插件把 Call Hierarchy 的 **Callers / Callees** 都做成了**递归**（不会在 Feign ↔ Controller 这一跳断开）。

## 配对

| 方向 | Feign 客户端（feign-order-client） | Controller（cloud-feign-server） |
|---|---|---|
| 完整路径 | `/hello/world/hierarchy/tree`（GET） | `/hello/world/hierarchy/tree`（GET） |
| 方法 | `HierarchyClient#tree` | `HierarchyServerController#tree` |

调用方链路（客户端侧）：`HierarchyCaller#run` → `HierarchyCaller#invoke` → `HierarchyClient#tree`
服务端调用链（服务端侧）：`HierarchyServerController#tree` → `HierarchyService#buildUser` → `HierarchyService#normalize`

文件：

```
feign-order-client/.../feign/client/HierarchyClient.java
feign-order-client/.../service/HierarchyCaller.java
cloud-feign-server/.../controller/HierarchyServerController.java
cloud-feign-server/.../service/HierarchyService.java
```

## 怎么测

把光标放在方法上按 `Ctrl+Alt+H`（Call Hierarchy），切换工具栏上的 **Callers / Callees** 视图并逐层展开。
打开时默认视图：Feign 方法打开 Callees，Controller 方法打开 Callers。

### 1) Callers 要越过 Feign 继续向上（回归点）

在 **`HierarchyServerController#tree`** 上打开 Call Hierarchy → **Callers**，预期：

```
HierarchyServerController#tree
└─ HierarchyClient#tree                 (Feign，作为 Controller 的远程调用者)
   └─ HierarchyCaller#invoke            (真正写代码调用 Feign 的地方)
      └─ HierarchyCaller#run            (再上一层调用者)
```

❌ 回归症状：只显示到 `HierarchyClient#tree` 为止，`HierarchyCaller#invoke / #run` 不出现。

### 2) Callees 要越过 Controller 继续向下（回归点）

在 **`HierarchyClient#tree`** 上打开 Call Hierarchy → **Callees**，预期：

```
HierarchyClient#tree
└─ HierarchyServerController#tree       (匹配的 Controller 端点)
   └─ HierarchyService#buildUser
      └─ HierarchyService#normalize
```

❌ 回归症状：只显示到 `HierarchyServerController#tree` 为止，服务端实现不出现。

## 另一个可复用（非专用）的组合

不新建文件也可用现有样例粗略验证：

- Callers：`UserServerController#getUserById` ← `UserClient#getUserById` ← `UserClientController#getUserById`
- Callees：`UserClient#getUserById` → `DuplicateUserServerController#getUserById` → `UserService#getUserById`

不过这两条链是别处用例的副产物，容易随其它改动变化，建议以本文件的专用用例为准。
