import { configureStore } from "@reduxjs/toolkit";
import { flightsApi } from "../features/flights/flightsApi";
import authReducer from "../features/auth/authSlice";

export const store = configureStore({
  reducer: {
    auth: authReducer,
    [flightsApi.reducerPath]: flightsApi.reducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(flightsApi.middleware),
});
