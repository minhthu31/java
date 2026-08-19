import React from 'react';
import TaskFilterBar from './TaskFilterBar';
import TaskLabel from './TaskLabel';

const MockTaskPage = () => {
  const handleFilter = (filters) => {
    console.log("Đang lọc danh sách với param:", filters);
  };

  return (
    <div style={{ padding: '20px', fontFamily: 'Arial, sans-serif' }}>
      <h2>CNPM-66: Demo UI Bộ lọc và Nhãn</h2>
      
      <TaskFilterBar onFilterChange={handleFilter} />

      <div style={{ marginTop: '20px' }}>
        <h3>Demo hiển thị nhãn Classification:</h3>
        <table border="1" cellPadding="10" style={{ borderCollapse: 'collapse', width: '50%' }}>
          <thead>
            <tr><th>Tên Task</th><th>Classification</th></tr>
          </thead>
          <tbody>
            <tr>
              <td>Làm giao diện danh sách</td>
              <td><TaskLabel classification="FRONTEND" /></td>
            </tr>
            <tr>
              <td>Viết API Task Filter</td>
              <td><TaskLabel classification="BACKEND" /></td>
            </tr>
            <tr>
              <td>Sửa lỗi crash khi query</td>
              <td><TaskLabel classification="BUG" /></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default MockTaskPage;