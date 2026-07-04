import { reactive } from "vue";
import type { FormRules } from "element-plus";

/** 用户名正则（用户名格式应为4-16位字母、数字、下划线、连字符的任意组合） */
export const REGEXP_USERNAME = /^[a-zA-Z0-9_-]{4,16}$/;

/** 密码正则（8-18位，至少包含字母、数字、特殊字符中的两种，允许下划线） */
export const REGEXP_PWD =
  /^(?![0-9]+$)(?![a-zA-Z]+$)(?![_$@!%*?&\-.,;#+=|()[\]{}^~`"'\\/]+$)[\S]{8,18}$/;

/** 登录校验 */
const loginRules = reactive(<FormRules>{
  username: [
    {
      validator: (rule, value, callback) => {
        if (value === "") {
          callback(new Error("请输入用户名"));
        } else if (!REGEXP_USERNAME.test(value)) {
          callback(new Error("4-16位字母、数字、下划线、连字符的任意组合"));
        } else {
          callback();
        }
      },
      trigger: "blur"
    }
  ],
  password: [
    {
      validator: (rule, value, callback) => {
        if (value === "") {
          callback(new Error("请输入密码"));
        } else if (!REGEXP_PWD.test(value)) {
          callback(new Error("8-18位数字、字母、符号的任意两种组合"));
        } else {
          callback();
        }
      },
      trigger: "blur"
    }
  ]
});

export { loginRules };
