import {Navigate} from 'react-router-dom'; import {currentUser} from './authService';
export default function ProtectedRoute({role,children}){const user=currentUser();if(!user||!localStorage.getItem('accessToken'))return <Navigate to="/login" replace/>;if(user.role!==role)return <Navigate to="/unauthorized" replace/>;return children;}
