import { redirect } from "next/navigation";
import { generateStaticParams } from "@/app/docs/[[...slug]]/page";

export default async function Page(props: PageProps<"/[[...slug]]">) {
  const params = await props.params;
  const slug = params.slug || [];
  const path = slug.join("/");
  redirect(`/docs/${path}`);
}

export { generateStaticParams };
