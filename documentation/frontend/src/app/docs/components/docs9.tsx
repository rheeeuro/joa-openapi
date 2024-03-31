import tw from "tailwind-styled-components";

export default function docs() {
  return (
    <>
      <Wrapper>
        <Title>버전 정보</Title>

        <Subtitle>최신 버전 정보</Subtitle>
        <TextItem>
          V 1.0
        </TextItem>

        <Subtitle>업데이트 내역</Subtitle>
        <TextItem>
          V 1.0 프로젝트 배포 (2024. 4. 3.)
        </TextItem>
      </Wrapper>

    </>
  );
};

const Wrapper = tw.div`
mt-8 space-y-6 text-sm
`;

const Title = tw.h2`
text-3xl font-bold
`;

const Subtitle = tw.h3`
text-xl font-bold
`;

const TextItem = tw.div`
leading-7 break-keep
`
