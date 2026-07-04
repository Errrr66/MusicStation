// 根据角色动态生成路由
import { defineFakeRoute } from "vite-plugin-fake-server/client";

export default defineFakeRoute([
  {
    url: "/login",
    method: "post",
    response: ({ body }) => {
      const isAdmin = body.username === "admin";
      // 构造 fake JWT 字符串，结构与后端真实返回保持一致（data 为 JWT 字符串）
      const payload = {
        claims: {
          role: isAdmin ? "admin" : "common",
          username: isAdmin ? "admin" : "common",
          avatar: "",
          nickname: isAdmin ? "admin" : "common",
          permissions: isAdmin
            ? ["*:*:*"]
            : ["permission:btn:add", "permission:btn:edit"]
        },
        exp: 2000000000
      };
      const encode = (obj: object) =>
        btoa(JSON.stringify(obj))
          .replace(/=/g, "")
          .replace(/\+/g, "-")
          .replace(/\//g, "_");
      const token = `${encode({ alg: "HS512" })}.${encode(payload)}.fake`;
      return {
        code: 0,
        message: "登录成功",
        data: token
      };
    }
  }
]);
