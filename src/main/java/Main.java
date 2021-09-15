import org.json.simple.*;
import org.json.simple.parser.*;
import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        List<Order> orders = new Main().new JsonSimpleParserEvent().parse("input.txt").getOrders();
        if (orders != null) {
            Iterator<Order> i = orders.iterator();
            while (i.hasNext()) {
                Order order = i.next();
                Iterator<Item> j = order.getItems().iterator();
                while (j.hasNext()) {
                    Item item = j.next();
                    if (item.getEvent().getStatus().equals("CANCEL") || item.getEvent() == null) {
                        j.remove();
                    }
                }
                if (order.getItems().size() == 0 || order.getItems() == null) {
                    i.remove();
                }
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"))) {
                writer.write(String.valueOf(orders));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            throw new Exception("Incorrect input data");
        }
    }

    public class Event implements Comparable<Event> {
        private int eventId; //идентификатор событияж
        private int orderId; //идентификатор заказа
        private int itemId; // идентификатор товара в заказе
        private int count; // Итоговое количество единиц товара в заказе, запрошенное к отправке
        private int returnCount; //Итоговое количество единиц товара в заказе, отмененное к отправке

        public String getStatus() {
            return status;
        }

        private String status; // статус товара в заказе.

        public Event(int eventId, int count, int returnCount, String status) {
            this.eventId = eventId;
            this.count = count;
            this.returnCount = returnCount;
            this.status = status;
        }

        public int getEventId() {
            return eventId;
        }

        @Override
        public String toString() {
            return String.valueOf(count - returnCount);
        }

        @Override
        public int compareTo(Event o) {
            return Integer.compare(eventId, o.getEventId());
        }
    }

    public class Item {
        private int itemId; // идентификатор товара в заказе
        private Event event; // актуальное событие

        public Item(int itemId, Event event) {
            this.itemId = itemId;
            this.event = event;
        }

        public int getItemId() {
            return itemId;
        }

        public Event getEvent() {
            return event;
        }

        public void setEvent(Event event) {
            this.event = event;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Item item = (Item) o;
            return itemId == item.itemId;
        }

        @Override
        public int hashCode() {
            return itemId;
        }

        @Override
        public String toString() {
            return System.lineSeparator() + "{" + System.lineSeparator()
                    + "\"count\": " + event + System.lineSeparator()
                    + "\"id\": " + itemId + "," + System.lineSeparator()
                    + '}';
        }
    }

    public class Order {
        private int orderId; //идентификатор заказа
        private List<Item> items = new ArrayList<>(); //коллекция товаров

        public Order(int orderId, Item item) {
            this.orderId = orderId;
            items.add(item);
        }

        public int getOrderId() {
            return orderId;
        }

        public List<Item> getItems() {
            return items;
        }

        public void addItem(Item item) {
            items.add(item);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Order order = (Order) o;
            return orderId == order.orderId;
        }

        @Override
        public int hashCode() {
            return orderId;
        }

        @Override
        public String toString() {
            return System.lineSeparator() + "{" + System.lineSeparator()
                    + "\"id\": " + orderId + "," + System.lineSeparator()
                    + "\"items\": " + items + System.lineSeparator()
                    + '}';
        }
    }

    public Item findItem(List<Item> items, int itemId) {
        for (Item item: items) {
            if(item.getItemId() == itemId) {
                return item;
            }
        }
        return null;
    }

    public class Root {
        private List<Order> orders;

        public List<Order> getOrders() {
            return orders;
        }

        public void setOrders(List<Order> orders) {
            this.orders = orders;
        }

        @Override
        public String toString() {
            return orders + System.lineSeparator();
        }
    }

    public Order findOrder(List<Order> orders, int orderId) {
        for (Order order: orders) {
            if(order.getOrderId() == orderId) {
                return order;
            }
        }
        return null;
    }

    public class JsonSimpleParserEvent {
        public Root parse(String filename) {
            Root root = new Root();
            JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(filename)) {
                JSONArray testsJsonArray = (JSONArray) parser.parse(reader);
                List<Order> orders = new ArrayList<>();
                for (Object event: testsJsonArray) {
                    JSONObject testJsonObject = (JSONObject) event;
                    int eventId = (int) ((long) testJsonObject.get("event_id"));
                    int orderId = (int) ((long) testJsonObject.get("order_id"));
                    int itemId = (int) ((long) testJsonObject.get("item_id"));
                    int count = (int) ((long) testJsonObject.get("count"));
                    int returnCount = (int) ((long) testJsonObject.get("return_count"));
                    String status = (String) testJsonObject.get("status");
                    if ((count - returnCount) > 0) {
                        Event newEvent = new Event(eventId, count, returnCount, status);
                        Order currOrder = findOrder(orders, orderId);
                        if (currOrder == null) {
                            orders.add(new Order(orderId, new Item(itemId, new Event(eventId, count, returnCount, status))));
                        } else {
                            Item currItem = findItem(currOrder.getItems(), itemId);
                            if (currItem == null) {
                                currOrder.addItem(new Item(itemId, new Event(eventId, count, returnCount, status)));
                            } else {
                                if (newEvent.compareTo(currItem.getEvent()) == 1) {
                                    currItem.setEvent(newEvent);
                                } else if (newEvent.compareTo(currItem.getEvent()) == 0) {
                                    throw new Exception("Inunique event!");
                                }
                            }
                        }
                    }
                }
                root.setOrders(orders);
                return root;
            } catch (IOException | ParseException | NullPointerException ex) {
                ex.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;
        }
    }
}
