<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head><meta charset="UTF-8"><title>부품 출고</title></head>
<body>
<h1>정비 부품 출고</h1>
<form method="post" action="/inventory/issue">
    <input name="transactionNo" placeholder="재고 거래번호">
    <input name="partNo" placeholder="부품번호">
    <input name="branchNo" placeholder="정비 지점번호">
    <input name="orderNo" placeholder="주문번호">
    <input name="quantity" placeholder="출고 수량">
    <button type="submit">부품 출고</button>
</form>
</body>
</html>
