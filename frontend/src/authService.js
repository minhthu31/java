import api from './api';
export async function login(credentials){const response=await api.post('/auth/login',credentials); return response.data.data;}
export function logout(){localStorage.removeItem('accessToken');localStorage.removeItem('currentUser');}
export function currentUser(){try{return JSON.parse(localStorage.getItem('currentUser'));}catch{return null;}}
