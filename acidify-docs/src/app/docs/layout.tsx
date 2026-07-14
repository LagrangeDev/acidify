import { GithubInfo } from "fumadocs-ui/components/github-info";
import { DocsLayout, type DocsLayoutProps } from "fumadocs-ui/layouts/docs";
import { baseOptions } from "@/lib/layout.shared";
import { gitConfig } from "@/lib/shared";
import { source } from "@/lib/source";

function docsOptions(): DocsLayoutProps {
  return {
    ...baseOptions(),
    tree: source.getPageTree(),
    links: [
      {
        type: "custom",
        children: <GithubInfo owner={gitConfig.user} repo={gitConfig.repo} />,
      },
    ],
  };
}

export default function Layout({ children }: LayoutProps<"/docs">) {
  return <DocsLayout {...docsOptions()}>{children}</DocsLayout>;
}
