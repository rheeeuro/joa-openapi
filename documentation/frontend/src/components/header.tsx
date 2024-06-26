"use client";

import tw from "tailwind-styled-components";
import Link from "next/link";
import { usePathname } from "next/navigation";

export default function Header() {
  const pathname = usePathname();
  return (
    <>
      <Wrapper>
        <Title>
          <Link href="/">
            <img className="w-[16rem]" src="/logo1.png" alt="joa-openapi" />
          </Link>
        </Title>
        <MenuContainer>
          <NavContainer>
            <NavItem $selected={pathname === "/home"}>
              <Link href="/">Home</Link>
            </NavItem>
            <NavItem $selected={pathname === "/docs"}>
              <Link href="/docs">Docs</Link>
            </NavItem>
            <NavItem $selected={pathname === "/testbeds"}>
              <Link href="/testbeds">APIs</Link>
            </NavItem>
          </NavContainer>
          <ButtonContainer>
            <ButtonItem>
              <a href="https://admin.joa13.site/">Admin</a>
            </ButtonItem>
          </ButtonContainer>
        </MenuContainer>
      </Wrapper>
    </>
  );
}

const Wrapper = tw.header`
p-8
lg:px-32
flex 
justify-between 
items-center
`;

const Title = tw.h1`
text-xl 
font-semibold
`;

const MenuContainer = tw.div`
flex
text-sm
`;

const NavContainer = tw.div`
flex 
items-center
bg-gray-100
rounded-full
px-6
py-2
mx-2
opacity-75
`;

const NavItem = tw.div<{ $selected: boolean }>`
w-10
flex
justify-center
items-center
m-2
mx-6
${(p) => (p.$selected ? "font-bold" : "hover:font-bold")}
`;

const ButtonContainer = tw.div`
flex 
items-center
bg-pink-300
text-white
rounded-full
px-6
py-2
mx-2
`;

const ButtonItem = tw.span`
m-2
hover:font-bold
`;
