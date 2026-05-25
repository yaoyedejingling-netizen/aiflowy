# AIFlowy 系统安装部署指南

- **适用对象**：项目经理、运维负责人、实施顾问
- **文档目的**：指导您或您的技术团队将 AIFlowy 系统顺利部署到生产服务器。



## 部署前准备清单

在开始之前，请确保您的服务器环境已满足以下基础条件：

1.  **服务器操作系统**：Linux (推荐 CentOS 7+ 或 Ubuntu 20.04+)
2.  **Java 环境**：必须安装 **JDK 17** 或更高版本。
3.  **构建工具**：
    *   后端编译需要安装 **Maven**。
    *   前端编译需要安装 **Node.js** 和 **pnpm包管理器**。
4.  **Web 服务器**：安装 **Nginx** 用于托管前端页面。
5.  **网络策略**：确保服务器防火墙已开放所需端口（默认后端端口通常为 8080，前端端口通常为 80 或 443）。

> 💡 **提示**：如果您不熟悉上述环境的安装，请联系您的系统管理员预先配置好基础环境。



## 第一步：后端服务部署（核心大脑）

后端是系统的“大脑”，负责处理数据和业务逻辑。

### 1. 配置文件修改
找到项目中的配置文件 `application-prod.yml`（位于后端项目的 `src/main/resources` 目录下）。
*   **操作**：根据实际生产环境修改数据库地址、Redis 地址、API 密钥等敏感信息。
*   **注意**：请务必仔细核对每一项配置，错误的配置会导致服务启动失败。

### 2. 打包后端程序
在项目根目录（包含 `pom.xml文件的文件夹），打开终端执行以下命令进行打包：
```bash
mvn clean package -DskipTests
```
*   **成功标志**：当控制台最后显示 `BUILD SUCCESS`字样时，表示打包成功。
*   **产物位置**：打包完成后，在 `aiflowy-starter-all/target` 目录下会生成一个名为 `aiflowy-starter-all-x.x.x.jar` 的文件（x.x.x 为版本号）。

### 3. 上传并启动服务
1.  将生成的 `.jar` 文件上传到您的 Linux 服务器指定目录（例如 `/aiflowy-v2/`）。
2.  在服务器上执行以下命令启动服务：

```bash
/java/jdk-17.0.8/bin/java -jar -Xmx1024M -Xms256M /aiflowy-v2/aiflowy-starter-all-x.x.x.jar --server.port=8080 --spring.profiles.active=prod
```

**参数说明（可根据实际情况调整）：**
*   `/java/jdk-17.0.8/bin/java`：如果您的服务器已配置全局 Java 环境变量，可直接替换为 `java`。
*   `-Xmx1024M -Xms256M`：内存设置，根据服务器配置适当调整。
*   `--server.port=8080`：后端服务运行的端口号，如需修改请更改此数字。
*   `--spring.profiles.active=prod`：指定使用生产环境配置。

> ✅ **验证方法**：启动后，在浏览器访问 `http://服务器IP:端口号/actuator/health`（如有配置健康检查接口）或通过日志查看是否有报错。



## 第二步：前端页面部署（用户界面）

前端是用户看到的“界面”，分为 **管理后台** 和 **用户中心** 两个部分。

### 1. 配置后端地址
前端需要知道后端服务的地址才能正常通信。

*   **管理后台配置**：
    1.  进入目录 `aiflowy-ui-admin/app/`。
    2.  打开文件 `.env.production`。
    3.  找到 `VITE_GLOB_API_URL` 这一行，将其值修改为您的**后端服务实际访问地址**（例如：`http://127.0.0.1:8080` 或公网域名）。

*   **用户中心配置**：
    1.  进入目录 `aiflowy-ui-usercenter/app/`。
    2.  打开文件 `.env.production`。
    3.  同样修改 `VITE_GLOB_API_URL` 为后端服务地址。

### 2. 编译前端代码
分别进入两个前端项目的根目录，执行编译命令：

**编译管理后台：**
```bash
cd aiflowy-ui-admin
pnpm build
```
*   编译成功后，在 `aiflowy-ui-admin/app` 目录下会生成 `dist.zip` 压缩包。

**编译用户中心：**
```bash
cd aiflowy-ui-usercenter
pnpm build
```
*   编译成功后，在 `aiflowy-ui-usercenter/app` 目录下会生成 `dist.zip` 压缩包。



## 第三步：Nginx 部署与配置

Nginx 作为 Web 服务器，负责展示前端页面并将请求转发给后端。

### 1. 准备前端文件
1.  在服务器上创建目录，例如 `/aiflowy/page`。
2.  将上一步生成的两个 `dist.zip` 文件上传到该目录。
3.  解压文件：
    ```bash
    cd /aiflowy/page
    unzip dist.zip
    ```
    *(注：如果两个前端项目需要区分目录，请分别解压到不同子文件夹，如 `/admin` 和 `/user`，并相应调整 Nginx 配置)*

### 2. 配置 Nginx
编辑 Nginx 配置文件（通常位于 `/etc/nginx/nginx.conf` 或 `/etc/nginx/conf.d/default.conf`），添加或修改如下配置：

```nginx
server {
    listen 80; # 监听端口
    server_name your-domain.com; # 替换为您的域名或服务器IP

    # 前端静态文件路径
    location / {
        root /aiflowy/page; # 指向您解压前端文件的目录
        try_files $uri $uri/ /index.html; # 支持前端路由刷新
    }

    # 后端接口反向代理（关键配置）
    # 当前端请求 /api 开头的内容时，转发给后端 Java 服务
    location /api {
        proxy_pass http://127.0.0.1:8080; # 替换为实际的后端地址和端口
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 3. 重启 Nginx
配置完成后，测试配置是否正确并重启服务：
```bash
nginx -t # 测试配置语法
nginx -s reload # 重载配置
```




## 常见问题排查

1.  **前端页面空白或无法加载？**
    *   检查浏览器控制台（F12）是否有报错。
    *   确认 `.env.production` 中的后端地址是否正确，且服务器之间网络互通。
    *   确认 Nginx 的 `root` 路径指向了正确的解压目录。

2.  **后端启动报错？**
    *   检查 JDK 版本是否为 17+。
    *   检查 `application-prod.yml` 中的数据库、Redis 连接信息是否正确。
    *   查看启动日志中的具体 Exception 信息。

3.  **接口请求 404 或 502？**
    *   检查 Nginx 配置中 `proxy_pass` 的地址是否与后端实际运行地址一致。
    *   确认后端服务是否正在运行，且端口未被防火墙拦截。



**技术支持**：
如在部署过程中遇到复杂问题，建议参考 Nginx 官方文档或联系 AIFlowy 技术支持团队。