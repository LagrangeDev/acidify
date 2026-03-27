import { Footer, LastUpdated, Layout, Navbar } from 'nextra-theme-docs';
import { getPageMap } from 'nextra/page-map';
import 'nextra-theme-docs/style.css';
import './styles.css';
import { Head, Search } from 'nextra/components';
import { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Acidify 文档',
  description: 'PC NTQQ 协议的 Kotlin 实现',
};

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh" suppressHydrationWarning>
    <Head />
    <body>
    <Layout
      navbar={
        <Navbar
          logo={
            <div style={{ fontSize: '1.15rem' }}>
              <b>Acidify</b> 文档
            </div>
          }
          projectLink={'https://github.com/LagrangeDev/acidify'}
        ></Navbar>
      }
      pageMap={[
        ...await getPageMap(),
        {
          name: 'kdoc',
          route: '/kdoc/index.html',
          title: 'acidify-core KDoc',
        }
      ]}
      docsRepositoryBase="https://github.com/LagrangeDev/acidify/tree/main/acidify-docs"
      search={
        <Search
          placeholder="搜索内容..."
          emptyResult="没有找到相关内容"
          errorText="加载索引失败"
          loading="加载中..."
        />
      }
      editLink="在 GitHub 上编辑此页"
      feedback={{
        content: '有问题？提交反馈',
        labels: 'documentation',
      }}
      lastUpdated={<LastUpdated locale="zh">最后更新于</LastUpdated>}
      themeSwitch={{
        dark: '暗色',
        light: '亮色',
        system: '跟随系统',
      }}
      toc={{
        title: '目录',
        backToTop: '返回顶部',
      }}
    >
      {children}
      <Footer>
        © {new Date().getFullYear()} LagrangeDev. Licensed under GNU GPLv3.
      </Footer>
    </Layout>
    </body>
    </html>
  );
}