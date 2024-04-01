import { adminLocalAxios } from "./http-common";

export const join = async (params: any): Promise<any> => {
  const response = await adminLocalAxios.post("", params);
  return response.data;
};

export const login = async (params: any): Promise<any> => {
  const response = await adminLocalAxios.post("/login", params);
  return response.data;
};
