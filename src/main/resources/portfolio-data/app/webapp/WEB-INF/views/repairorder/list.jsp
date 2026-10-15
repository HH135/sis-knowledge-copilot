<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head><meta charset="UTF-8"><title>정비 주문</title></head>
<body>
<h1>정비 주문 접수</h1>
<form method="post" action="/repair-order/create">
    <label>주문번호 <input name="orderNo"></label>
    <label>예약번호 <input name="appointmentNo"></label>
    <button type="submit">주문 생성</button>
</form>
</body>
</html>
