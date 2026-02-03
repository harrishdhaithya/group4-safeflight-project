import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";

export const flightsApi = createApi({
  reducerPath: "flightsApi",
  baseQuery: fetchBaseQuery({
    baseUrl: "http://localhost:8080/api",
  }),
  endpoints: (builder) => ({
    searchFlights: builder.query({
      query: (params) => ({
        url: "/flights/search",
        params,
      }),
    }),
  }),
});

export const { useSearchFlightsQuery } = flightsApi;
