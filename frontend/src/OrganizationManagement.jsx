import React, { useCallback, useEffect, useState } from "react";
import api from "./api";

const emptyPerson = { username: "", email: "", fullName: "", initialPassword: "" };

export default function OrganizationManagement({ role }) {
    const admin = role === "ADMIN";
    const [groups, setGroups] = useState([]);
    const [lecturers, setLecturers] = useState([]);
    const [selected, setSelected] = useState(null);
    const [members, setMembers] = useState([]);
    const [person, setPerson] = useState(emptyPerson);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    const groupBase = admin ? "/admin/groups" : "/lecturer/groups";
    const load = useCallback(async () => {
        try {
            setError("");
            const groupResponse = await api.get(groupBase);
            setGroups(groupResponse.data.data || []);
            if (admin) {
                const lecturerResponse = await api.get("/admin/lecturers");
                setLecturers(lecturerResponse.data.data || []);
            }
        } catch (e) { setError(e.response?.data?.message || "Không tải được dữ liệu tổ chức."); }
    }, [admin, groupBase]);

    useEffect(() => { load(); }, [load]);

    const chooseGroup = async (group) => {
        setSelected(group);
        const response = await api.get(`${groupBase}/${group.id}/members`);
        setMembers(response.data.data || []);
    };

    const createGroup = async (event) => {
        event.preventDefault();
        const form = new FormData(event.currentTarget);
        await api.post("/admin/groups", { code: form.get("code"), name: form.get("name"), status: "ACTIVE" });
        event.currentTarget.reset(); setMessage("Đã tạo nhóm."); await load();
    };

    const createPerson = async (kind) => {
        const path = kind === "lecturer" ? "/admin/lecturers" : `${groupBase}/${selected.id}/members`;
        await api.post(path, person); setPerson(emptyPerson); setMessage("Đã tạo tài khoản và cập nhật danh sách.");
        await load(); if (selected) await chooseGroup(selected);
    };

    const assignLecturer = async (lecturerId) => {
        await api.put(`/admin/groups/${selected.id}/lecturers/${lecturerId}`);
        setMessage("Đã phân công giảng viên."); await load();
    };

    const removeMember = async (userId) => {
        await api.delete(`${groupBase}/${selected.id}/members/${userId}`);
        setMessage("Đã ngừng tư cách thành viên và gỡ Task chưa hoàn thành."); await chooseGroup(selected);
    };

    const run = async (action) => { try { setError(""); await action(); } catch (e) { setError(e.response?.data?.message || "Thao tác thất bại."); } };

    return <section style={{padding:24}}>
        <h2>{admin ? "Quản trị nhóm và giảng viên" : "Quản lý sinh viên được phân công"}</h2>
        {message && <p role="status" style={{color:"#15803d"}}>{message}</p>}
        {error && <p role="alert" style={{color:"#b91c1c"}}>{error}</p>}
        {admin && <form onSubmit={(e)=>run(()=>createGroup(e))} style={{display:"flex",gap:8,marginBottom:16}}>
            <input name="code" required placeholder="Mã nhóm"/><input name="name" required placeholder="Tên nhóm"/>
            <button type="submit">Tạo nhóm</button>
        </form>}
        <div style={{display:"grid",gridTemplateColumns:"minmax(260px,1fr) 2fr",gap:20}}>
            <div><h3>Nhóm</h3>{groups.map(g=><button key={g.id} onClick={()=>run(()=>chooseGroup(g))}
                style={{display:"block",width:"100%",padding:10,marginBottom:6,textAlign:"left"}}>
                {g.code} — {g.name} ({g.memberCount} SV)
            </button>)}</div>
            <div>{selected ? <>
                <h3>{selected.code} — {selected.name}</h3>
                {admin && <div><strong>Giảng viên:</strong> {(selected.lecturers||[]).map(x=>x.fullName).join(", ")||"Chưa phân công"}
                    <select defaultValue="" onChange={e=>e.target.value&&run(()=>assignLecturer(Number(e.target.value)))}>
                        <option value="">+ Phân công</option>{lecturers.map(x=><option key={x.id} value={x.id}>{x.fullName}</option>)}
                    </select></div>}
                <h4>Sinh viên</h4>
                <table><thead><tr><th>Họ tên</th><th>Username</th><th>Email</th><th></th></tr></thead>
                    <tbody>{members.map(x=><tr key={x.id}><td>{x.fullName}</td><td>{x.username}</td><td>{x.email}</td>
                        <td><button onClick={()=>run(()=>removeMember(x.id))}>Gỡ khỏi nhóm</button></td></tr>)}</tbody></table>
                <h4>Tạo sinh viên</h4><PersonForm value={person} setValue={setPerson} onSave={()=>run(()=>createPerson("member"))}/>
            </> : <p>Chọn một nhóm để quản lý.</p>}</div>
        </div>
        {admin && <div style={{marginTop:24}}><h3>Tạo giảng viên</h3>
            <PersonForm value={person} setValue={setPerson} onSave={()=>run(()=>createPerson("lecturer"))}/></div>}
    </section>;
}

function PersonForm({value,setValue,onSave}) {
    const field=(name,label,type="text")=><input type={type} value={value[name]} placeholder={label}
        onChange={e=>setValue({...value,[name]:e.target.value})}/>;
    return <div style={{display:"flex",gap:8,flexWrap:"wrap"}}>
        {field("username","Username")}{field("email","Email","email")}{field("fullName","Họ tên")}
        {field("initialPassword","Mật khẩu ban đầu","password")}<button type="button" onClick={onSave}>Lưu</button>
    </div>;
}
