/**
 * VitePress 站点配置
 * 文档：https://vitepress.dev/zh/reference/site-config
 */
import { defineConfig } from 'vitepress'

// GitHub Pages 部署时通过环境变量覆盖：DOCS_BASE=/engineering-notes/ npm run build
const base = process.env.DOCS_BASE || '/'

export default defineConfig({
  base,
  lang: 'zh-CN',
  title: '前后端工程化学习笔记',
  description: '按模块有序管理的 Spring Boot 后端 + Vue 前端学习笔记，代码可运行、结论可复现',

  cleanUrls: true,
  lastUpdated: true,

  vite: {
    server: { host: '0.0.0.0', allowedHosts: true },
    preview: { host: '0.0.0.0', allowedHosts: true }
  },

  head: [
    ['link', { rel: 'icon', type: 'image/svg+xml', href: `${base}logo.svg` }],
    ['meta', { name: 'theme-color', content: '#3eaf7c' }]
  ],

  markdown: {
    lineNumbers: true,
    theme: {
      light: 'github-light',
      dark: 'github-dark'
    },
    // 代码块标题：```java [ConfigController.java]
    headers: {
      includeLevel: [2, 3]
    }
  },

  themeConfig: {
    siteTitle: '工程化学习笔记',

    outline: {
      level: [2, 3],
      label: '本页目录'
    },

    nav: [
      { text: '首页', link: '/' },
      { text: '开始', link: '/guide/', activeMatch: '^/guide/' },
      { text: '后端', link: '/backend/', activeMatch: '^/backend/' },
      { text: '前端', link: '/frontend/', activeMatch: '^/frontend/' },
      { text: '工程化', link: '/engineering/', activeMatch: '^/engineering/' },
      { text: '内容规划', link: '/guide/outline' }
    ],

    sidebar: {
      '/guide/': [
        {
          text: '开始',
          items: [
            { text: '课程总览', link: '/guide/' },
            { text: '学习路线', link: '/guide/roadmap' },
            { text: '笔记内容规划', link: '/guide/outline' },
            { text: '开发环境与工具链', link: '/guide/env' },
            { text: '工程规范约定', link: '/guide/conventions' },
            { text: '学习心得', link: '/guide/reflection' }
          ]
        }
      ],

      '/backend/': [
        {
          text: '后端总览',
          items: [{ text: '模块地图', link: '/backend/' }]
        },
        {
          text: '基础篇',
          collapsed: false,
          items: [
            { text: '01 快速入门', link: '/backend/01-quickstart' },
            { text: '02 配置管理', link: '/backend/02-config/' },
            { text: '03 日志管理', link: '/backend/03-logging' },
            { text: '04 Web 开发', link: '/backend/04-web' }
          ]
        },
        {
          text: '数据篇',
          collapsed: false,
          items: [
            { text: '05 MySQL', link: '/backend/05-mysql' },
            { text: '06 MyBatis', link: '/backend/06-mybatis' },
            { text: '07 消息队列', link: '/backend/07-mq' }
          ]
        },
        {
          text: '进阶篇',
          collapsed: false,
          items: [
            { text: '08 定时任务', link: '/backend/08-schedule' },
            { text: '09 安全认证', link: '/backend/09-security' },
            { text: '10 文件处理', link: '/backend/10-file' },
            { text: '11 接口文档', link: '/backend/11-doc' },
            { text: '12 单元测试', link: '/backend/12-test' },
            { text: '13 监控运维', link: '/backend/13-actuator' }
          ]
        }
      ],

      '/backend/02-config/': [
        {
          text: '02 配置管理',
          items: [
            { text: '模块总览', link: '/backend/02-config/' },
            { text: '@Value 注入详解', link: '/backend/02-config/value' },
            { text: '@ConfigurationProperties', link: '/backend/02-config/properties' },
            { text: '多环境与 Profile', link: '/backend/02-config/profile' },
            { text: '配置校验与最佳实践', link: '/backend/02-config/validation' }
          ]
        }
      ],

      '/frontend/': [
        {
          text: '前端总览',
          items: [{ text: '知识地图', link: '/frontend/' }]
        },
        {
          text: 'Web 三件套',
          items: [
            { text: '01 HTML 与 CSS', link: '/frontend/01-html-css' },
            { text: '02 JavaScript 与 ES6+', link: '/frontend/02-js' }
          ]
        },
        {
          text: 'Vue 3',
          items: [
            { text: '03 CDN 字面量入门', link: '/frontend/03-vue-literal' },
            { text: '04 综合案例：TodoList', link: '/frontend/04-todolist' },
            { text: '05 Vite 工程化项目', link: '/frontend/05-vite-project' }
          ]
        }
      ],

      '/engineering/': [
        {
          text: '工程化',
          items: [
            { text: '总览', link: '/engineering/' },
            { text: 'Maven 多模块工程', link: '/engineering/maven-multimodule' },
            { text: 'Git 工作流', link: '/engineering/git-workflow' },
            { text: 'IDEA 工程配置', link: '/engineering/idea-setup' },
            { text: '踩坑与排错记录', link: '/engineering/troubleshooting' }
          ]
        }
      ]
    },

    search: {
      provider: 'local',
      options: {
        translations: {
          button: { buttonText: '搜索笔记', buttonAriaLabel: '搜索笔记' },
          modal: {
            noResultsText: '没有找到相关结果',
            resetButtonTitle: '清除查询条件',
            footer: { selectText: '选择', navigateText: '切换', closeText: '关闭' }
          }
        }
      }
    },

    docFooter: {
      prev: '上一篇',
      next: '下一篇'
    },

    lastUpdated: {
      text: '最后更新于',
      formatOptions: { dateStyle: 'short', timeStyle: 'short' }
    },

    darkModeSwitchLabel: '主题',
    lightModeSwitchTitle: '切换到浅色模式',
    darkModeSwitchTitle: '切换到深色模式',
    sidebarMenuLabel: '目录',
    returnToTopLabel: '回到顶部',
    externalLinkIcon: true,

    footer: {
      message: '基于 VitePress 构建 · 笔记随学习进度持续更新',
      copyright: 'Copyright © 2026 a1788'
    }
  }
})
