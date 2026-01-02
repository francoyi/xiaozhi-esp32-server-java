import { createRouter, createWebHistory } from 'vue-router'
import { authStore } from '../store/auth'

// 页面
import RoleCardsPage from '../pages/Home/RoleCardsPage.vue'
import DeviceListPage from '../pages/Devices/DeviceListPage.vue'
import CreateRolePage from '../pages/Roles/CreateRolePage.vue'
import EditRolePage from '../pages/Roles/EditRolePage.vue'
import DeviceConsolePage from '../pages/Console/DeviceConsolePage.vue'
import ComingSoonPage from '../pages/Common/ComingSoonPage.vue'
import MePage from '../pages/Me/MePage.vue'

import RegisterPage from '../pages/Auth/RegisterPage.vue'
import LoginPage from '../pages/Auth/LoginPage.vue'
import ScanPage from '../pages/Auth/ScanPage.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    // 🔐 认证页
    { path: '/login', component: LoginPage },
    { path: '/register', component: RegisterPage },
    { path: '/scan', component: ScanPage },

    // 主应用
    { path: '/home', component: RoleCardsPage },
    { path: '/devices', component: DeviceListPage },
    // 先保留路由，但用移动端占位页，避免出现“表格乱码”
    { path: '/configs', component: ComingSoonPage, props: { title: '配置', hint: '该页面后续再做移动端优化' } },
    { path: '/roles', redirect: '/home' },
    { path: '/roles/create', component: CreateRolePage },
    { path: '/roles/edit/:id', component: EditRolePage },

    { path: '/console', component: DeviceConsolePage },
    { path: '/console/:deviceId', component: DeviceConsolePage },

    { path: '/me', component: MePage },

    // 根路径：交给守卫决定跳哪
    { path: '/', redirect: '/home' },
  ],
})

// 路由白名单（不需要登录也能访问）
const WHITE_LIST = new Set(['/login', '/register', '/scan'])

router.beforeEach((to, _from, next) => {
  const token = authStore.getToken()

  // 1️⃣ 未登录 → 只能进 login / register
  if (!token && !WHITE_LIST.has(to.path)) {
    return next('/login')
  }

  // 2️⃣ 已登录 → 不允许再进 login / register
  if (token && WHITE_LIST.has(to.path)) {
    return next('/home')
  }

  // 3️⃣ 正常放行
  next()
})

