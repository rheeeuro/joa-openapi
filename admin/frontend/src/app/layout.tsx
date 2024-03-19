import RecoilContextProvider from "./recoilContextProvider";

export const metadata = {
  title: "Next.js",
  description: "Generated by Next.js",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body id={"root"}>
        <RecoilContextProvider>{children}</RecoilContextProvider>
      </body>
    </html>
  );
}