import React, { useState, useEffect } from 'react';

const TaskFilterBar = ({ onFilterChange }) => {
  const [users, setUsers] = useState([]);
  const [filters, setFilters] = useState({
    type: '',
    status: '',
    assignee: '',
    priority: ''
  });

  useEffect(() => {
    // Giả lập API gọi danh sách User để không bị hard-code
    const fetchUsers = async () => {
      const mockUsers = [
        { id: 1, name: 'Nguyễn Văn A (Frontend)' },
        { id: 2, name: 'Trần Thị B (Backend)' }
      ];
      setUsers(mockUsers);
    };
    fetchUsers();
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    const updatedFilters = { ...filters, [name]: value };
    setFilters(updatedFilters);
    
    if (onFilterChange) {
      onFilterChange(updatedFilters);
    }
  };

  const selectStyle = {
    padding: '6px 12px',
    borderRadius: '4px',
    border: '1px solid #d9d9d9',
    marginRight: '10px',
    minWidth: '150px'
  };

  return (
    <div style={{ padding: '16px', backgroundColor: '#f8f9fa', borderRadius: '8px', marginBottom: '20px' }}>
      <strong style={{ marginRight: '15px' }}>Bộ Lọc Task:</strong>
      
      <select name="type" value={filters.type} onChange={handleChange} style={selectStyle}>
        <option value="">-- Tất cả Type --</option>
        <option value="TASK">Task</option>
        <option value="BUG">Bug</option>
        <option value="STORY">Story</option>
      </select>

      <select name="status" value={filters.status} onChange={handleChange} style={selectStyle}>
        <option value="">-- Tất cả Trạng thái --</option>
        <option value="TO_DO">To Do</option>
        <option value="IN_PROGRESS">In Progress</option>
        <option value="DONE">Done</option>
      </select>

      <select name="priority" value={filters.priority} onChange={handleChange} style={selectStyle}>
        <option value="">-- Tất cả Độ ưu tiên --</option>
        <option value="HIGH">High</option>
        <option value="MEDIUM">Medium</option>
        <option value="LOW">Low</option>
      </select>

      <select name="assignee" value={filters.assignee} onChange={handleChange} style={selectStyle}>
        <option value="">-- Tất cả Người phụ trách --</option>
        {users.map(user => (
          <option key={user.id} value={user.id}>{user.name}</option>
        ))}
      </select>
    </div>
  );
};

export default TaskFilterBar;