export default function AccountGroupSearch() {
  return (
    <div>
      <div className="relative mt-2 mr-3 rounded-md shadow-sm">
        <input
          type="text"
          name="searchQuery"
          id="searchQuery"
          className="flex w-full rounded-md border-0 py-1.5 pl-28 pr-1 text-gray-900 ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm sm:leading-6"
          placeholder=""
        />
        <div className="absolute inset-y-0 left-0 flex items-center">
          <select
            // value={searchType}
            className="h-full rounded-md border-0 bg-transparent py-0 pl-4 pr-2 text-gray-500 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm"
          >
            <option value="ACCOUNT_ID">계좌번호</option>
            <option value="ACCOUNT_NAME">계좌이름</option>
            <option value="HOLDER_NAME">고객이름</option>
            <option value="PRODUCT_NAME">상품이름</option>
            <option value="DUMMY_NAME">더미이름</option>
          </select>
        </div>
      </div>
    </div>
  );
}
