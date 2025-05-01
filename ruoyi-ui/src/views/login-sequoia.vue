<template>
  <div class="login" @contextmenu.prevent @click="showForm" v-if="!formVisible">
    <!-- Removed the profile image completely -->
  </div>
  <div class="login" @contextmenu.prevent v-else>
    <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
      <h3 class="title">欢迎使用生态价值核算平台</h3>
      <el-form-item prop="username">
        <el-input
          v-model="loginForm.username"
          type="text"
          auto-complete="off"
          placeholder="账号"
        >
          <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="loginForm.password"
          type="password"
          auto-complete="off"
          placeholder="密码"
          @keyup.enter.native="handleLogin"
        >
          <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="code" v-if="captchaEnabled">
        <el-input
          v-model="loginForm.code"
          auto-complete="off"
          placeholder="验证码"
          style="width: 63%"
          @keyup.enter.native="handleLogin"
        >
          <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" />
        </el-input>
        <div class="login-code">
          <img :src="codeUrl" @click="getCode" class="login-code-img"/>
        </div>
      </el-form-item>
      <el-checkbox v-model="loginForm.rememberMe" style="margin:0px 0px 25px 0px;">记住密码</el-checkbox>
      <el-form-item style="width:100%;">
        <el-button
          :loading="loading"
          size="medium"
          type="primary"
          class="login-button"
          @click.native.prevent="handleLogin"
        >
          <span v-if="!loading">登 录</span>
          <span v-else>登 录 中...</span>
        </el-button>
        <div style="float: right;" v-if="register">
          <router-link class="link-type" :to="'/register'">立即注册</router-link>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { getCodeImg } from "@/api/login";
import Cookies from "js-cookie";
import { encrypt, decrypt } from '@/utils/jsencrypt'

export default {
  name: "Login",
  data() {
    return {
      formVisible: false,
      codeUrl: "",
      loginForm: {
        username: "admin",
        password: "admin123",
        rememberMe: false,
        code: "",
        uuid: ""
      },
      loginRules: {
        username: [
          { required: true, trigger: "blur", message: "请输入您的账号" }
        ],
        password: [
          { required: true, trigger: "blur", message: "请输入您的密码" }
        ],
        code: [{ required: true, trigger: "change", message: "请输入验证码" }]
      },
      loading: false,
      // 验证码开关
      captchaEnabled: true,
      // 注册开关
      register: false,
      redirect: undefined
    };
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect;
      },
      immediate: true
    }
  },
  created() {
    this.getCode();
    this.getCookie();
  },
  methods: {
    showForm() {
      this.formVisible = true;
    },
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled;
        if (this.captchaEnabled) {
          this.codeUrl = "data:image/gif;base64," + res.img;
          this.loginForm.uuid = res.uuid;
        }
      });
    },
    getCookie() {
      const username = Cookies.get("username");
      const password = Cookies.get("password");
      const rememberMe = Cookies.get('rememberMe')
      this.loginForm = {
        username: username === undefined ? this.loginForm.username : username,
        password: password === undefined ? this.loginForm.password : decrypt(password),
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
      };
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true;
          if (this.loginForm.rememberMe) {
            Cookies.set("username", this.loginForm.username, { expires: 30 });
            Cookies.set("password", encrypt(this.loginForm.password), { expires: 30 });
            Cookies.set('rememberMe', this.loginForm.rememberMe, { expires: 30 });
          } else {
            Cookies.remove("username");
            Cookies.remove("password");
            Cookies.remove('rememberMe');
          }
          this.$store.dispatch("Login", this.loginForm).then(() => {
            this.$router.push({ path: this.redirect || "/" }).catch(()=>{});
          }).catch(() => {
            this.loading = false;
            if (this.captchaEnabled) {
              this.getCode();
            }
          });
        }
      });
    }
  }
};
</script>

<style rel="stylesheet/scss" lang="scss">
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  overflow: hidden;
}

.login {
  height: 100vh;
  width: 100vw;
  background-image: url('../assets/images/Sequoia.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  position: relative;
  overflow: hidden;
}

.title {
  margin: 0px auto 30px auto;
  text-align: center;
  color: #ffffff;
  background-color: #2c5282;
  padding: 15px 0;
  border-radius: 4px;
  font-size: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.login-form {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.95);
  width: 400px;
  padding: 25px 25px 5px 25px;
  box-shadow: 0 5px 25px rgba(0, 0, 0, 0.2);
  
  .el-input {
    height: 42px;
    
    input {
      height: 42px;
      background-color: #f5f7fa;
      border-radius: 4px;
      border: 1px solid #e4e7ed;
      
      &:focus {
        border-color: #2c5282;
      }
    }
  }
  
  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 2px;
    color: #5e6d82;
  }
  
  .el-checkbox {
    color: #5e6d82;
  }
  
  .login-button {
    width: 100%;
    background-color: #2c5282;
    border-color: #2c5282;
    height: 42px;
    font-size: 16px;
    
    &:hover, &:focus {
      background-color: #3c73b3;
      border-color: #3c73b3;
    }
  }
}

.login-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}

.login-code {
  width: 33%;
  height: 38px;
  float: right;
  
  img {
    cursor: pointer;
    vertical-align: middle;
    border-radius: 4px;
  }
}

.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #fff;
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
}

.login-code-img {
  height: 42px;
  border-radius: 4px;
}
</style>
