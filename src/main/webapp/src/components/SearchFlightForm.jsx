import { Form, Input, Button } from "antd";

export default function SearchFlightForm() {
  const onFinish = (values) => {
    console.log("Flight Search Data:", values);
  };

  return (
    <div className="bg-white p-6 rounded shadow-md w-full max-w-md">
      <h2 className="text-xl font-semibold mb-4">
        Search Flights ✈️
      </h2>

      <Form layout="vertical" onFinish={onFinish}>
        <Form.Item
          label="From"
          name="from"
          rules={[{ required: true, message: "Enter source city" }]}
        >
          <Input placeholder="e.g. Delhi" />
        </Form.Item>

        <Form.Item
          label="To"
          name="to"
          rules={[{ required: true, message: "Enter destination city" }]}
        >
          <Input placeholder="e.g. Mumbai" />
        </Form.Item>

        <Form.Item>
          <Button type="primary" htmlType="submit" block>
            Search Flights
          </Button>
        </Form.Item>
      </Form>
    </div>
  );
}
