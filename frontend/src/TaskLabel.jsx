import React from 'react';

const TaskLabel = ({ classification }) => {
  const labelStyles = {
    FRONTEND: { bg: '#e6f7ff', color: '#1890ff', border: '#91d5ff' },
    BACKEND: { bg: '#f6ffed', color: '#52c41a', border: '#b7eb8f' },
    BUG: { bg: '#fff2f0', color: '#ff4d4f', border: '#ffccc7' },
    DEFAULT: { bg: '#f5f5f5', color: '#595959', border: '#d9d9d9' }
  };

  const style = labelStyles[classification] || labelStyles.DEFAULT;

  return (
    <span style={{
      backgroundColor: style.bg,
      color: style.color,
      border: `1px solid ${style.border}`,
      padding: '2px 8px',
      borderRadius: '4px',
      fontSize: '12px',
      fontWeight: '600',
      display: 'inline-block'
    }}>
      {classification || 'UNKNOWN'}
    </span>
  );
};

export default TaskLabel;