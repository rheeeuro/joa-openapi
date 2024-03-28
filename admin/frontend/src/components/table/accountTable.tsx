import tw from "tailwind-styled-components";
import { useRouter } from "next/navigation";
import { FaSort } from "react-icons/fa";
import { useEffect, useState } from "react";
import axios, { AxiosInstance, AxiosResponse } from "axios";
import { localAxios } from "@/api/http-common";
import { useQuery } from "@tanstack/react-query";
import { getAccountList } from "@/api/Account";

export default function AccountTable() {
  interface Account {
    accountId: string;
    accountName: string;
    balance: number;
    isDormant: boolean;
    transferLimit: number;
    startDate: string;
    endDate: string;
    term: number;
    depositAccount: string;
    withdrawAccount: string;
    amount: number;
    holderName: string;
    productName: string | null;
    dummyName: string | null;
    bankId: string;
    createdAt: string;
    updatedAt: string;
  }

  const Accounts = [
    {
      id: 1,
      nickname: "예금통장",
      customerName: "조아영",
      productName: "청년희망정기예금",
      startDate: "2023-02-02",
      endDate: "2023-12-02",
      balance: 100000,
      recentTransactionAmount: 100000,
    },
    {
      id: 2,
      nickname: "예금통장",
      customerName: "조아영",
      productName: "청년희망정기예금",
      startDate: "2023-02-02",
      endDate: "2023-12-02",
      balance: 100000,
      recentTransactionAmount: 100000,
    },
    {
      id: 3,
      nickname: "예금통장",
      customerName: "조아영",
      productName: "청년희망정기예금",
      startDate: "2023-02-02",
      endDate: "2023-12-02",
      balance: 100000,
      recentTransactionAmount: 100000,
    },
    {
      id: 4,
      nickname: "예금통장",
      customerName: "조아영",
      productName: "청년희망정기예금",
      startDate: "2023-02-02",
      endDate: "2023-12-02",
      balance: 100000,
      recentTransactionAmount: +100000,
    },
    {
      id: 5,
      nickname: "예금통장",
      customerName: "조아영",
      productName: "청년희망정기예금",
      startDate: "2023-02-02",
      endDate: "2023-12-02",
      balance: 100000,
      recentTransactionAmount: -100000,
    },
  ];

  const [accountList, setAccountList] = useState<Account[]>([]);

  const api: AxiosInstance = axios.create({
    baseURL: "https://joa13.site/v1", // JSON 데이터를 가져올 엔드포인트의 URL
    headers: {
      apiKey: "9b5c450f-abd4-419f-b092-bcd96e66392f",
      "Content-Type": "application/json",
    },
  });

  const {data} = useQuery({queryKey: ['accountList'], queryFn: getAccountList});



  const router = useRouter();

  return (
    <div className="relative overflow-x-auto shadow-md sm:rounded-lg">
      <table className="w-full text-sm text-left text-gray-500 ">
        <thead className="text-xs text-gray-700 uppercase bg-gray-50">
          <tr>
            <th scope="col" className="px-6 py-3">
              계좌별명
            </th>
            <th scope="col" className="px-6 py-3">
              고객이름
            </th>
            <th scope="col" className="px-6 py-3">
              상품명
            </th>
            <th scope="col" className="px-6 py-3  gap-3">
              <span className="flex gap-3">
                신규일자
                <FaSort></FaSort>
              </span>
            </th>
            <th scope="col" className="px-6 py-3">
              <span className="flex gap-3">
                만기일자
                <FaSort></FaSort>
              </span>
            </th>
            <th scope="col" className="px-6 py-3 gap-3">
              <span className="flex gap-3">
                잔액
                <FaSort></FaSort>
              </span>
            </th>
            <th scope="col" className="px-6 py-3">
              최근거래금액
            </th>
            <th scope="col" className="relative px-6 py-3">
              <span className="sr-only"> </span>
            </th>
          </tr>
        </thead>
        <tbody>
          {Accounts.map((account) => (
            <tr
              key={account.id}
              className="border-b transition duration-300 ease-in-out hover:bg-neutral-100"
            >
              <TableData className="font-medium text-gray-900 whitespace-nowrap">
                {account.nickname}
              </TableData>
              <TableData>{account.customerName}</TableData>
              <TableData>{account.productName}</TableData>
              <TableData>{account.startDate}</TableData>
              <TableData>{account.endDate}</TableData>
              <TableData>{account.balance}</TableData>
              <td
                className={`px-6 py-4 ${
                  account.recentTransactionAmount >= 0
                    ? "text-green-600"
                    : "text-red-600"
                }`}
              >
                {account.recentTransactionAmount >= 0 ? "+" : "-"}
                {Math.abs(account.recentTransactionAmount).toLocaleString()}
              </td>
              <td className="px-6 py-4">
                <a
                  onClick={() => {
                    router.push(`account/detail`);
                  }}
                  className="font-medium text-pink-400 hover:text-pink-500 cursor-pointer"
                >
                  자세히
                </a>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

const TableHeader = tw.th`

`;

const TableData = tw.td`
px-6 
py-4
`;
